import time
import os
import subprocess
import audioop
from dataclasses import dataclass
import speech_recognition as sr
import pyttsx3
import dashscope
try:
    from dashscope.version import __version__ as dashscope_version
except ImportError:
    dashscope_version = None
from dashscope import Application
from dashscope.audio.asr.recognition import Recognition, RecognitionCallback
from http import HTTPStatus

# ================= 配置区域 =================
# 打印当前 dashscope SDK 版本，便于定位兼容性问题
DASHSCOPE_SDK_VERSION = getattr(dashscope, '__version__',
                                dashscope_version or 'unknown')
print(f"[INFO] dashscope SDK version: {DASHSCOPE_SDK_VERSION}")

# 1. 设置您的阿里云百炼 API KEY
dashscope.api_key = 'sk-fb64515c017945fc9282f9ace355cad3' 

# 2. 设置您的应用 ID (从您截图的控制台获取)
APP_ID = '16356830643247938dfa31f8414fd58d' 
# ===========================================

# 千问 ASR 模型配置（切换到 16k 版本，效果更稳）
# 官方模型 ID 使用英文标识，避免 ModelNotFound
QWEN_ASR_MODEL = 'paraformer-realtime-8k-v2'
QWEN_ASR_SAMPLE_RATE = 8000

# 简单 ASR 回调占位，主要用于满足 Recognition 构造要求
class SimpleASRCallback(RecognitionCallback):
    def on_open(self): pass
    def on_complete(self): pass
    def on_error(self, result): pass
    def on_close(self): pass
    def on_event(self, result): pass

# 播放状态标记（半双工）
is_playing: bool = False

# 连续流式录音，基于能量检测的简单 VAD：有声继续录，静音超过阈值后停止
def record_with_vad(source,
                    max_duration: float = 30.0,
                    silence_duration: float = 2,
                    chunk_size: int = 1024):
    frames = []
    start_time = time.time()
    last_voice_time = time.time()

    while True:
        if time.time() - start_time > max_duration:
            break
        try:
            data = source.stream.read(chunk_size)
        except OverflowError:
            continue
        frames.append(data)
        energy = audioop.rms(data, source.SAMPLE_WIDTH)
        if energy > r.energy_threshold:
            last_voice_time = time.time()
        if (time.time() - last_voice_time) >= silence_duration and len(frames) > 5:
            break

    raw_data = b"".join(frames)
    return sr.AudioData(raw_data, source.SAMPLE_RATE, source.SAMPLE_WIDTH)


# 使用千问 ASR 识别本地音频（wav/pcm）
def qwen_asr_transcribe(audio_bytes: bytes,
                        sample_rate: int = QWEN_ASR_SAMPLE_RATE) -> str | None:
    temp_path = 'temp_input.wav'
    try:
        with open(temp_path, 'wb') as f:
            f.write(audio_bytes)

        asr = Recognition(model=QWEN_ASR_MODEL,
                          callback=SimpleASRCallback(),
                          format='wav',
                          sample_rate=sample_rate)
        result = asr.call(temp_path)
        sentence = result.get_sentence()
        print(f"[DEBUG] ASR status={result.status_code}, code={result.code}, msg={result.message}, raw_sentence={sentence}")

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


# 初始化语音识别器 ASR 
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

    with sr.Microphone(sample_rate=QWEN_ASR_SAMPLE_RATE) as source:
        print("\n👂 正在聆听... (请说话)")
        # 自动调整环境噪音
        r.adjust_for_ambient_noise(source, duration=0.5)
        try:
            print("Processing (VAD 模式，静音将自动结束)...")
            audio = record_with_vad(source,
                                    max_duration=30.0,
                                    silence_duration=1.2,
                                    chunk_size=1024)
            wav_data = audio.get_wav_data(convert_rate=QWEN_ASR_SAMPLE_RATE,
                                          convert_width=2)
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

def chat_with_agent(prompt):
    """调用百炼智能体"""
    try:
        response = Application.call(
            app_id=APP_ID,
            prompt=prompt,
            # session_id 可以在这里维护以实现多轮对话记忆，本示例简化处理
        )

        if response.status_code != HTTPStatus.OK:
            print(f'❌ 请求失败: {response.message}')
            return "对不起，我现在有点头晕，请稍后再试。"
        
        return response.output.text

    except Exception as e:
        print(f"❌ 调用异常: {e}")
        return "系统出错了。"

def main():
    speak("你好，我是你的智能管家。请问有什么可以帮你的？")
    
    while True:
        # 1. 听
        user_text = listen()
        
        if user_text:
            # 退出机制
            if "再见" in user_text or "退出" in user_text:
                speak("好的，再见！")
                break
            
            # 2. 想 (发送给百炼)
            llm_end_ts = None
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
