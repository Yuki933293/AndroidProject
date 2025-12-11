package com.luxshare.bluetooth.thread

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.luxshare.bluetooth.IBluetoothService
import com.luxshare.bluetooth.listener.IReadListener
import com.luxshare.bluetooth.constants.Constants

/**
 * The usb read thread.
 *
 * @author ChenCe
 * @version version
 */
class BluetoothReadThread(
    private val bluetoothService: IBluetoothService,
    private val readListener: IReadListener
) : Thread() {
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var readHandler: Handler? = null
    private var stop = false
    private val readRunnable: Runnable = object : Runnable {
        override fun run() {
            if (stop) {
                return
            }
            val readBuffer = bluetoothService.read()
            if (readBuffer == null) {
                usbReadFailed()
            } else {
                if (readBuffer.isNotEmpty()) {
                    usbReadSuccess(readBuffer)
                }
            }
            readHandler?.postDelayed(this, Constants.READ_PERIOD)
        }
    }

    private fun usbReadFailed() {
        mainHandler.post {
            readListener.onReadFailure() // 读取失败回调
        }
    }

    private fun usbReadSuccess(readBuffer: ByteArray) {
        mainHandler.post {
            readListener.onReadSuccess(readBuffer) // 读取成功回调
        }
    }

    override fun run() {
        Log.d(TAG, "usbhidlog -> " + "UsbReadThread is running")
        Looper.prepare()
        readHandler = Looper.myLooper()?.let { Handler(it) }
        readHandler?.post(readRunnable)
        Looper.loop()
    }

    fun stopThread() {
        stop = true
        readHandler?.removeCallbacks(readRunnable)
    }

    companion object {
        private val TAG = BluetoothReadThread::class.java.simpleName
    }
}