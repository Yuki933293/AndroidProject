package com.luxshare.bluetooth.listener

/**
 * @desc 蓝牙数据回调
 *
 * @author hudebo
 * @date 2023/12/12
 */
interface IBluetoothDataCallBack {
    fun blueDataCallBack(readBuffer: ByteArray)
}