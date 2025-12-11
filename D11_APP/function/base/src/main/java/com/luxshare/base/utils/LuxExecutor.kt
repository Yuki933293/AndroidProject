package com.luxshare.base.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * @desc 功能描述
 * @author hudebo
 * @date 2023/11/30
 */
object LuxExecutor {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executorService = Executors.newSingleThreadExecutor()

    fun runOnAsrThread(runnable: Runnable?) {
        executorService.submit(runnable)
    }

    fun runOnUiThread(r: Runnable?): Boolean {
        return mainHandler.post(r!!)
    }

    fun runOnuUiThreadDelay(r: Runnable?, delay: Long): Boolean {
        return mainHandler.postDelayed(r!!, delay)
    }

    fun removeUiRunnable(r: Runnable?) {
        mainHandler.removeCallbacks(r!!)
    }
}