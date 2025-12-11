package com.luxshare.base.utils
import android.content.Context
import android.media.AudioManager

/**
 * @author hudebo
 * @desc 声音控制类
 * @date 2023/3/14
 */
object VolumeUtil {
    private const val TAG = "VolumeUtil"

    //设置媒体音量 刻度100
    fun setVolume(context: Context, streamType: Int,value: Int, flags: Int) {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        kotlin.runCatching {
            manager.run {
                val maxVolume = getStreamMaxVolume(streamType)
                val volume = value.div(maxVolume.toFloat()).times(100).toInt()
                setStreamVolume(streamType, volume, flags)
            }
            manager.setStreamVolume(streamType, value.div(manager.getStreamMaxVolume(streamType).toFloat())
                .times(100).toInt(), flags)
        }
    }

    fun getMaxVolume(context: Context, streamType: Int): Int {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getStreamMaxVolume(streamType)
    }

    //开启静音模式
    fun muteSystem(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, true)
    }

    //关闭静音模式
    fun unMuteSystem(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.setStreamMute(AudioManager.STREAM_MUSIC, false)
    }

    //获取当前媒体音量
    fun getCurrentVolume(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    //音量增加
    fun volumeUp(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
        )
    }

    //音量降低
    fun volumeDown(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
        )
    }

    //自动调整音量
    fun bootResetVolume(context: Context) {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (manager != null) {
            //当前音量
            val volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
            try {
                Thread.sleep(100)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            //与最大音量比较
            if (volume != manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                //向音量增加方向调整
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                )
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                )
            } else {
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                )
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
                )
            }
        }
    }
}