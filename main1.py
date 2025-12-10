import time
import subprocess
from dataclasses import dataclass
import io
import wave
import os

import numpy as np
import torch
import speech_recognition as sr
import dashscope
import requests  # 用于调用心知天气 API
import pyaudio
import struct
from openwakeword.model import Model as OWWModel
from openwakeword.model import Model as OWWModel

try:
    from dashscope.version import __version__ as dashscope_version
except ImportError:
    dashscope_version = None

from dashscope import Application
from dashscope.audio.asr.recognition import Recognition, RecognitionCallback
from http import HTTPStatus
import uuid

# ================= 配置区域 =================
# 打印当前 dashscope SDK 版本，便于定位兼容性问题
DASHSCOPE_SDK_VERSION = getattr(
    dashscope, '__version__', dashscope_version or 'unknown'
)
print(f"[INFO] dashscope SDK version: {DASHSCOPE_SDK_VERSION}")

# 1. 设置阿里云百炼 API KEY（这里用占位符，记得换成你自己的）
dashscope.api_key = 'sk-fb64515c017945fc9282f9ace355cad3'

# 2. 设置你的应用 ID
APP_ID = '16356830643247938dfa31f8414fd58d'

# 3. 心知天气配置（直接写在代码里；注意不要泄露到公开仓库）
#    key 直接用私钥方式：...now.json?key=your_private_key&location=beijing&language=zh-Hans&unit=c
SENIVERSE_KEY = "Sr1izeSrxPYouIE7B"  # TODO: 替换成你自己的心知天气私钥
SENIVERSE_WEATHER_NOW_URL = "https://api.seniverse.com/v3/weather/now.json"
# ===========================================
# openWakeWord 唤醒词配置
OWW_MODEL_PATHS = ["wakewords/xiaoguanjia.tflite"]  # 自定义唤醒词模型路径
OWW_THRESHOLD = 0.5  # 触发阈值，可按需要调整
# 会话 ID（多轮记忆）
SESSION_ID = str(uuid.uuid4())

# 采样率设置：全部统一到 16k
MIC_SAMPLE_RATE = 16000
# VAD 宿醉参数
HANGOVER_GAP_SEC = 0.6   # 语音片段间小于该间隔则合并
HANGOVER_PAD_SEC = 0.4   # 前后各扩展的缓冲

# 千问 ASR 模型配置：16k 版本
QWEN_ASR_MODEL = 'paraformer-realtime-v2'
QWEN_ASR_SAMPLE_RATE = 16000  # 与模型保持一致
WAKE_WORDS = ["小管家", "助手", "你好", "嗨"]  # 简易唤醒词列表
oww_model: OWWModel | None = None

# ---------- Silero VAD 初始化 ----------
print("[INFO] 正在加载 Silero VAD 模型...")
silero_model, silero_utils = torch.hub.load(
    repo_or_dir='snakers4/silero-vad',
    model='silero_vad',
    force_reload=False
)
(get_speech_timestamps,
 _,
 _,
 _,
 _) = silero_utils
print("[INFO] Silero VAD 模型加载完成。")

# 播放状态标记（半双工）
is_playing: bool = False

# ---------- ASR 回调占位 ----------
class SimpleASRCallback(RecognitionCallback):
    def on_open(self): pass
    def on_complete(self): pass
    def on_error(self, result): pass
    def on_close(self): pass
    def on_event(self, result): pass


# ---------- 使用 Silero VAD 裁剪语音 ----------
def apply_silero_vad_to_audio(audio: sr.AudioData) -> sr.AudioData:
    """
    输入：speech_recognition 的 AudioData（内部转为 16k）
    过程：
      1. 转为 16k 单声道 wav bytes
      2. 提取 PCM 数据，转为 torch.Tensor
      3. 用 Silero VAD 获取语音区间
      4. 截取 [第一个 start, 最后一个 end] 之间的语音
      5. 返回新的 AudioData（16k, 16bit）
    """
    # 1. 从 AudioData 获取 16k wav bytes
    wav_bytes = audio.get_wav_data(
        convert_rate=MIC_SAMPLE_RATE,
        convert_width=2  # 16-bit
    )

    # 2. 用 wave 模块解析 PCM
    with wave.open(io.BytesIO(wav_bytes), 'rb') as wf:
        num_channels = wf.getnchannels()
        assert num_channels == 1, f"期望单声道音频，实际通道数={num_channels}"
        sample_width = wf.getsampwidth()
        assert sample_width == 2, f"期望 16bit 音频，实际位宽={sample_width}"
        sample_rate = wf.getframerate()
        assert sample_rate == MIC_SAMPLE_RATE, f"采样率不匹配: {sample_rate}"
        num_frames = wf.getnframes()
        pcm_bytes = wf.readframes(num_frames)

    # 3. PCM bytes -> torch.Tensor (float32, -1~1)
    pcm16 = np.frombuffer(pcm_bytes, dtype=np.int16)
    if pcm16.size == 0:
        print("[VAD] PCM 数据为空")
        return audio

    audio_tensor = torch.from_numpy(pcm16).float() / 32768.0

    # 4. Silero VAD 获取语音时间戳
    speech_ts = get_speech_timestamps(
        audio_tensor,
        silero_model,
        sampling_rate=MIC_SAMPLE_RATE
    )

    if not speech_ts:
        print("[VAD] Silero 未检测到语音，返回原始 AudioData。")
        return audio

    # Hangover 机制：合并间隔很短的语音段，并在首尾加缓冲
    hangover_gap = int(HANGOVER_GAP_SEC * MIC_SAMPLE_RATE)
    pad = int(HANGOVER_PAD_SEC * MIC_SAMPLE_RATE)

    merged = []
    cur_start, cur_end = speech_ts[0]['start'], speech_ts[0]['end']
    for seg in speech_ts[1:]:
        if seg['start'] - cur_end <= hangover_gap:
            cur_end = max(cur_end, seg['end'])
        else:
            merged.append({'start': cur_start, 'end': cur_end})
            cur_start, cur_end = seg['start'], seg['end']
    merged.append({'start': cur_start, 'end': cur_end})

    start = max(0, merged[0]['start'] - pad)
    end = min(len(audio_tensor), merged[-1]['end'] + pad)
    print(f"[VAD] 合并后语音片段: start={start}, end={end}, 总长度={len(audio_tensor)}")

    voiced = audio_tensor[start:end]

    # 5. 再转回 int16 PCM bytes
    voiced = torch.clamp(voiced, -1.0, 1.0)
    voiced_int16 = (voiced * 32768.0).short().numpy().tobytes()

    # 6. 封装为新的 AudioData（16k, 16bit）
    trimmed_audio = sr.AudioData(
        voiced_int16,
        MIC_SAMPLE_RATE,
        2  # sample width
    )
    return trimmed_audio


# ---------- 使用千问 ASR 识别本地音频 ----------
def qwen_asr_transcribe(audio_bytes: bytes,
                        sample_rate: int = QWEN_ASR_SAMPLE_RATE) -> str | None:
    temp_path = 'temp_input.wav'
    try:
        with open(temp_path, 'wb') as f:
            f.write(audio_bytes)

        asr = Recognition(
            model=QWEN_ASR_MODEL,
            callback=SimpleASRCallback(),
            format='wav',
            sample_rate=sample_rate
        )
        result = asr.call(temp_path)
        sentence = result.get_sentence()
        print(f"[DEBUG] ASR status={result.status_code}, "
              f"code={result.code}, msg={result.message}, raw_sentence={sentence}")

        text = None
        if isinstance(sentence, list):
            # 取最后一句有效文本
            for item in reversed(sentence):
                if isinstance(item, dict):
                    text = item.get('text') or item.get('result')
                    if text:
                        break
        elif isinstance(sentence, dict):
            text = sentence.get('text') or sentence.get('result')

        if text:
            return text

        print(f"[DEBUG] ASR 返回无法解析: {sentence}")
        return None
    except Exception as e:
        print(f"[DEBUG] 千问 ASR 调用异常: {e}")
        return None


# ---------- 心知天气：实况天气工具 ----------
def get_weather_from_seniverse(location: str) -> str:
    """
    调用心知天气实况天气接口，返回一段面向用户的中文描述。
    文档示例：
      https://api.seniverse.com/v3/weather/now.json?key=your_api_key&location=beijing&language=zh-Hans&unit=c
    """
    if not SENIVERSE_KEY:
        return "天气查询功能尚未配置心知天气密钥。"

    try:
        params = {
            "key": SENIVERSE_KEY,   # 私钥
            "location": location,   # 可以是城市名、拼音、经纬度等
            "language": "zh-Hans",
            "unit": "c"
        }
        resp = requests.get(SENIVERSE_WEATHER_NOW_URL, params=params, timeout=5)
        resp.raise_for_status()
        data = resp.json()

        # 若返回错误结构：{"status": "...", "status_code": "AP010010"}
        if not isinstance(data, dict):
            return f"查询 {location} 的天气时返回了异常数据。"

        if "results" not in data:
            status = data.get("status")
            status_code = data.get("status_code")
            if status or status_code:
                print(f"[DEBUG] Seniverse error: status={status}, code={status_code}")
                return f"心知天气接口返回错误：{status or '未知错误'}（{status_code or '无错误码'}）。"
            return f"查询 {location} 的天气失败，未获得有效结果。"

        results = data.get("results") or []
        if not results:
            return f"没有找到 {location} 的天气信息。"

        first = results[0]
        loc = first.get("location", {}) or {}
        now = first.get("now", {}) or {}
        last_update = first.get("last_update")

        # location 信息，尽量只保留一个城市名，避免重复
        path = loc.get("path")
        name = loc.get("name")
        place = None
        if path:
            # path 示例："西雅图,华盛顿州,美国"，仅取第一段城市
            first = str(path).split(",")[0].strip()
            place = first or None
        if not place and name:
            place = str(name).strip()
        if not place:
            place = location

        text = now.get("text")            # 天气现象文字，例如“多云”
        temperature = now.get("temperature")
        humidity = now.get("humidity")
        wind_speed = now.get("wind_speed")
        wind_direction = now.get("wind_direction")

        parts = []
        if place:
            parts.append(f"{place} 当前天气：{text or '暂无'}")
        else:
            parts.append(f"{location} 当前天气：{text or '暂无'}")

        if temperature is not None:
            parts.append(f"气温 {temperature}℃")
        if humidity is not None:
            parts.append(f"湿度 {humidity}%")
        if wind_direction or wind_speed is not None:
            # 风向/风速有哪个说哪个
            wind_desc = []
            if wind_direction:
                wind_desc.append(wind_direction)
            if wind_speed is not None:
                wind_desc.append(f"{wind_speed} 公里/小时")
            if wind_desc:
                parts.append("风向风速：" + " ".join(wind_desc))
        if last_update:
            parts.append(f"（数据更新时间：{last_update}）")

        return "，".join(parts) + "。"

    except Exception as e:
        print(f"[DEBUG] Seniverse 请求异常: {e}")
        return f"查询 {location} 的天气时出错了，请稍后再试。"


def extract_city_from_text(text: str) -> str | None:
    """
    简单规则解析“城市 + 天气”：
      - 如果句子里包含“天气”两字，则尝试把 “天气” 前后的文字当作地名。
      - 例：
        “上海天气怎么样”          -> 城市：上海
        “帮我查一下北京的天气”    -> 城市：北京
        “今天天气怎么样”          -> 解析不出城市，返回 None
    """
    if "天气" not in text:
        return None

    text = text.strip()
    idx = text.find("天气")

    # 情形1："...城市...天气..."
    if idx > 0:
        before = text[:idx]
        for ch in ["的", "现在", "今日", "今天", "今儿", "一下", "查查", "帮我", "帮我查", "帮忙查"]:
            before = before.replace(ch, "")
        city = before.strip()
        if city:
            return city

    # 情形2："天气...城市..."（较少见）
    after = text[idx + len("天气"):]
    for ch in ["的", "现在", "今日", "今天", "今儿", "一下", "查查", "帮我", "帮我查", "帮忙查"]:
        after = after.replace(ch, "")
    after = after.strip()
    if after:
        return after

    return None
# ---------- /心知天气：实况天气工具 ----------


# 初始化语音识别器
r = sr.Recognizer()


def speak(text, t_llm_last: float | None = None):
    print(f"🤖 智能体正在生成语音: {text} ...")
    global is_playing
    is_playing = True
    try:
        # 使用 macOS 内置 say 命令进行 TTS 播放
        subprocess.run(["say", text], check=False)
    finally:
        is_playing = False


def listen():
    # 半双工：若机器人在播报，先等待播报结束
    while is_playing:
        time.sleep(0.05)

    with sr.Microphone(sample_rate=MIC_SAMPLE_RATE) as source:
        print("\n👂 正在聆听... (请说话)")
        # 自动调整环境噪音，此时提醒用户先不要说话
        r.adjust_for_ambient_noise(source, duration=1.0)
        # 调整静音判定，避免过早截断
        r.pause_threshold = 1.2
        r.non_speaking_duration = 0.3
        r.energy_threshold = max(r.energy_threshold, 150)

        try:
            print("开始录音（speech_recognition + Silero VAD 裁剪）...")
            # 使用 speech_recognition 的监听逻辑
            audio = r.listen(
                source,
                timeout=12,            # 最多等用户 12 秒开始说话
                phrase_time_limit=30   # 单次最长录音 30 秒
            )

            # 用 Silero VAD 进一步裁剪头尾静音
            trimmed_audio = apply_silero_vad_to_audio(audio)

            # 直接以 16k 输出 wav bytes，交给 16k ASR
            wav_data = trimmed_audio.get_wav_data(
                convert_rate=QWEN_ASR_SAMPLE_RATE,
                convert_width=2
            )

            text = qwen_asr_transcribe(wav_data)
            if text:
                print(f"👤 您说: {text}")
            else:
                print("...ASR 未识别出有效文本")
            return text

        except sr.WaitTimeoutError:
            print("...超时未检测到语音")
            return None
        except sr.UnknownValueError:
            print("...无法理解音频")
            return None


def listen_wakeup() -> bool:
    """使用 openWakeWord 进行唤醒词检测，检测到返回 True"""
    global oww_model
    if oww_model is None:
        try:
            # 新版本参数为 wakeword_models
            model_paths = [p for p in OWW_MODEL_PATHS if p and os.path.exists(p)]
            if not model_paths:
                print(f"[WAKE] 未找到唤醒词模型文件：{OWW_MODEL_PATHS}")
                return False
            oww_model = OWWModel(wakeword_models=model_paths)
            print(f"[WAKE] openWakeWord 模型加载完成: {model_paths}")
        except Exception as e:
            print(f"[WAKE] openWakeWord 初始化失败: {e}")
            return False

    sample_rate = getattr(oww_model, "sample_rate", MIC_SAMPLE_RATE)
    frame_length = getattr(oww_model, "window_samples", 512)

    pa = pyaudio.PyAudio()
    stream = pa.open(rate=sample_rate,
                     channels=1,
                     format=pyaudio.paInt16,
                     input=True,
                     frames_per_buffer=frame_length)

    print("[WAKE] 正在监听唤醒词（openWakeWord）...")
    triggered = False
    end_time = time.time() + 15  # 最多监听 15 秒
    try:
        while time.time() < end_time:
            pcm = stream.read(frame_length, exception_on_overflow=False)
            audio_int16 = np.frombuffer(pcm, dtype=np.int16)
            preds = oww_model.predict(audio_int16)
            if preds and any(score >= OWW_THRESHOLD for score in preds.values()):
                print(f"[WAKE] 检测到唤醒词（openWakeWord）：{preds}")
                triggered = True
                break
    except Exception as e:
        print(f"[WAKE] openWakeWord 监听异常: {e}")
    finally:
        stream.stop_stream()
        stream.close()
        pa.terminate()

    return triggered


def chat_with_agent(prompt):
    """调用百炼智能体"""
    try:
        response = Application.call(
            app_id=APP_ID,
            prompt=prompt,
            session_id=SESSION_ID  # 传入固定 session_id，实现多轮记忆
        )

        if response.status_code != HTTPStatus.OK:
            print(f'❌ 请求失败: {response.message}')
            return "对不起，我现在有点头晕，请稍后再试。"

        return response.output.text

    except Exception as e:
        print(f"❌ 调用异常: {e}")
        return "系统出错了。"


def main():
    print("等待唤醒词中...")

    while True:
        # 先等待唤醒词
        if not listen_wakeup():
            continue

        # 唤醒后问候一次
        speak("你好，我是你的智能管家。请问有什么可以帮你的？")

        # 进入对话循环
        while True:
            user_text = listen()

            if user_text:
                # 退出机制
                if "再见" in user_text or "退出" in user_text:
                    speak("好的，再见！")
                    break

                # ===== 天气工具路由逻辑（心知天气）=====
                city = extract_city_from_text(user_text)
                if city:
                    print(f"[ROUTER] 检测到天气查询，城市 = {city}")
                    reply_text = get_weather_from_seniverse(city)
                    llm_end_ts = time.time()
                else:
                    # 非天气问题，交给百炼智能体
                    reply_text = chat_with_agent(user_text)
                    llm_end_ts = time.time()

                # 3. 说
                speak(reply_text, t_llm_last=llm_end_ts)

            # 简单防刷屏等待
            time.sleep(0.5)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n程序已手动停止")
