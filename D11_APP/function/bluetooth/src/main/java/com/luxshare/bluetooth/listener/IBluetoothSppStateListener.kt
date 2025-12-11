package com.luxshare.bluetooth.listener

/**
 * @desc 功能描述
 * @author hudebo
 * @date 2023/12/11
 */
interface IBluetoothSppStateListener {
    /**
     * SPP连接成功
     *
     * @param device
     */
    fun onBluetoothSPPConnected()

    /**
     * SPP连接失败
     *
     * @param device
     */
    fun onBluetoothSPPDisConnected()
}