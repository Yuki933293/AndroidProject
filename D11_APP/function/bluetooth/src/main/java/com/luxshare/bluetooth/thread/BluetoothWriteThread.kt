package com.luxshare.bluetooth.thread

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.luxshare.bluetooth.IBluetoothService
import com.luxshare.bluetooth.listener.IWriteListener
import com.luxshare.bluetooth.constants.Constants

/**
 * The usb write thread.
 *
 * @author ChenCe
 * @version version
 */
class BluetoothWriteThread(
    private val writeQueue: WriteQueue<ByteArray>,
    private val bluetoothService: IBluetoothService,
    private val bleWriteListener: IWriteListener, ) : Thread() {

    private val TAG = this::class.java.simpleName
    private val mainHandler = Handler(Looper.getMainLooper())
    private var writeHandler: Handler? = null
    private var stop = false
    private val writeRunnable: Runnable = object : Runnable {
        override fun run() {
            if (stop) {
                return
            }

            //不为空
            if (!writeQueue.isEmpty) {
                if (stop) {
                    return
                }
                try {
                    val bytes = writeQueue.remove()
                    if (bytes != null) {
                        // 队列取数据成功
                        val ret: Boolean = bluetoothService.write(bytes)
                         Log.d(TAG, "run: write bytes:result:${ret}")

                        // 写入操作
                        if (ret) {
                            writeSuccess() // 写入成功
                        } else {
                            writeFailed() // 写入失败
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "run: ", e)
                    writeFailed() // 写入失败
                }
            }
            writeHandler?.postDelayed(this, Constants.WRITE_PERIOD) //是否需要延时?
        }
    }

    private fun writeSuccess() {
        mainHandler.post { bleWriteListener.onWriteSuccess() }
    }

    private fun writeFailed() {
        mainHandler.post { bleWriteListener.onWriteFailure() }
    }

    override fun run() {
        Log.d(TAG, "BleWriteThread is running")
        Looper.prepare()
        writeHandler = Looper.myLooper()?.let { Handler(it) }
        writeHandler?.post(writeRunnable)
        Looper.loop()
    }

    fun stopThread() {
        stop = true
        writeHandler?.removeCallbacks(writeRunnable)
    }
}