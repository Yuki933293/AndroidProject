package com.luxshare.base.utils

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.media.*
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlin.math.pow


/**
 * @desc 录音播放
 * @author hudebo
 * @date 2023/3/15
 */
class RecordPlayUtil(context: Context) : Runnable {
    private val TAG = "RecordPlayUtil"
    private val SAMPLE_RATE_DEFAULT = 8000
    private var thread: Thread? = null
    private val bufferSize
        get() = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_DEFAULT,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

    private val mRecordBufferSize
        get() = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_DEFAULT,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

    private var mAudioRecord: AudioRecord? = null

    private var mAudioTrack: AudioTrack? = null

    private var animationDrawable: AnimationDrawable? = null

    init {
        mAudioRecord = if (ActivityCompat.checkSelfPermission(
                context, permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "create AudioRecord failed")
            null
        } else {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE_DEFAULT,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, mRecordBufferSize
            )
        }
        mAudioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE_DEFAULT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "create mAudioTrack failed")
            null
        }
    }

    fun start(animationDrawable: AnimationDrawable) {
        Log.i(TAG, "start")
        this.animationDrawable = animationDrawable
        thread = Thread(this)
        thread!!.start()
    }

    fun stop() {
        Log.i(TAG, "stop")
        kotlin.runCatching {
            mAudioRecord?.let {
                it.stop()
                it.release()
                mAudioRecord = null
            }

            mAudioTrack?.let {
                it.stop()
                it.release()
                mAudioTrack = null
            }
        }.onFailure {
            Log.e(TAG, "stop error", it)
        }
        animationDrawable?.stop()
        thread?.interrupt()
    }

    override fun run() {
        kotlin.runCatching {
            var callbackNumber = 0
            mAudioRecord?.let { audioRecord ->
                val buffer = ByteArray(bufferSize)
                var size: Int
                audioRecord.startRecording()
                mAudioTrack?.play()
                animationDrawable?.start()
                while (audioRecord.read(buffer, 0, bufferSize).also {
                        size = it
                    } != -1) {
                    mAudioTrack?.let {
                        //Log.i(TAG, "run: size: $size")
                        if ((callbackNumber++ == 0) && size > 0) {
                            recordCallback?.let { callback ->
                                callback.canReadData()
                            }
                        }
                        it.write(buffer, 0, size)
                        calculateVol(buffer)
                    }
                }
                Log.i(TAG, "animationDrawable stop")
                animationDrawable?.stop()
            }
        }.onFailure {
            Log.e(TAG, "unable to record and play", it)
            animationDrawable?.stop()
        }
    }

    /**
     * 获取分贝大小
     */
    private fun calculateVol(buffer: ByteArray): Double {
        var value: Long = 0
        for (i in buffer.indices) {
            value += buffer[i].toDouble().pow(2.0).toLong()
        }

        val volume = 20 * kotlin.math.log10(value.div(buffer.size.toDouble()))
        //Log.i(TAG, "volume:$volume")
        return volume
    }

    private var recordCallback: RecordCallback? = null
    fun setRecordCallback(recordCallback: RecordCallback?) {
        this.recordCallback = recordCallback
    }

    interface RecordCallback {
        fun canReadData()
    }
}
