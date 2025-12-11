package com.luxshare.bluetooth.constants

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date 2023/12/6
 */
object Constants {
    const val READ_PERIOD: Long = 0
    const val WRITE_PERIOD: Long = 0

    const val BLUETOOTH_READ_SIZE: Int = 1024

    const val BLUETOOTH_ACTION_GATT_CONNECTED = "com.luxshare.bluetooth.ACTION_GATT_CONNECTED"
    const val BLUETOOTH_ACTION_GATT_CONNECTING = "com.luxshare.bluetooth.ACTION_GATT_CONNECTING"
    const val BLUETOOTH_ACTION_GATT_DISCONNECTED = "com.luxshare.bluetooth.ACTION_GATT_DISCONNECTED"
    const val BLUETOOTH_ACTION_GATT_SOCKET_DISCOVERED = "com.luxshare.bluetooth.ACTION_GATT_socket_DISCOVERED"
    const val BLUETOOTH_ACTION_DATA_AVAILABLE = "com.luxshare.bluetooth.ACTION_DATA_AVAILABLE"

    // SPP uuid
    const val BLUETOOTH_SERVICE_UUID = "00001108-0000-1000-8000-00805f9b34fb"

}