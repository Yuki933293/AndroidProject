package com.luxshare.bluetooth.listener

import android.bluetooth.BluetoothDevice

/**
 * @desc 功能描述
 * @author hudebo
 * @date 2023/12/6
 */
interface IBluetoothScanCallBack {
    fun callBack(device: BluetoothDevice)
    fun callBackEnd()
}