package com.luxshare.bluetooth.listener

import android.bluetooth.BluetoothDevice

/**
 * @desc 功能描述
 * @author hudebo
 * @date 2023/12/11
 */
interface IBluetoothDeviceStateListener {
    /**
     * 设备绑定状态
     *
     * @param state [Bluetooth.BOND_BONDED]
     */
    fun onBluetoothSPPBondStateChanged(device: BluetoothDevice?)

    /**
     * 设备连接成功
     *
     * @param device
     */
    fun onBluetoothConnected(device: BluetoothDevice?)

    /**
     * 设备断开连接
     *
     * @param device
     */
    fun onBluetoothDisconnected(device: BluetoothDevice?)
}