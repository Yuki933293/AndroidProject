package com.luxshare.home.repository

import android.util.Log
import com.luxshare.ble.BleManager
import com.luxshare.ble.interfaces.IBluetoothDataCallBack
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2024/12/5 10:39
 */
class MeetingRepository {
    private val TAG = "MeetingRepository"

    private var start = false

    private var bluetoothDataCallBack: IBluetoothDataCallBack? = null

    fun fetchMeetingData(byteArray: ByteArray): Flow<ByteArray> = callbackFlow {
        bluetoothDataCallBack = object : IBluetoothDataCallBack {
            override fun blueDataCallBack(readBuffer: ByteArray) {
                if (!start) {
                    return
                }
                try {
                    trySend(readBuffer)
                } catch (e: Exception) {
                    Log.e(TAG,"数据解析或关闭出现异常", e)
                }
            }
        }
        start = true
        BleManager.getInstance().addBluetoothDataCallBack(TAG, bluetoothDataCallBack)
        BleManager.getInstance().write(byteArray)
        awaitClose {
            Log.d(TAG,"数据发送完成")
        }
    }

    fun pause() {
        start = false
        Log.d(TAG, "pause")
    }

    fun release() {
        BleManager.getInstance().removeBluetoothDataCallBack(TAG)
        bluetoothDataCallBack = null
    }
}