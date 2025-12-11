package com.luxshare.ble

object Constants {
    const val CONNECT = "connect"
    const val BLE_BONDED_ADDRESS_SET = "ble bonded address set"
    const val BLUETOOTH_CURRENT_ADDRESS = "bluetooth current address"

    const val READ_PERIOD: Long = 80
    const val WRITE_PERIOD: Long = 200

    // Stops scanning after 10 seconds.
    const val BLE_SCAN_PERIOD: Long = 30000
    const val BLE_ACTION_GATT_CONNECTED = "com.luxshare.bluetooth.le.ACTION_GATT_CONNECTED"
    const val BLE_ACTION_GATT_CONNECTING = "com.luxshare.bluetooth.le.ACTION_GATT_CONNECTING"
    const val BLE_ACTION_GATT_DISCONNECTED = "com.luxshare.bluetooth.le.ACTION_GATT_DISCONNECTED"
    const val BLE_ACTION_GATT_SERVICES_DISCOVERED =
        "com.luxshare.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
    const val BLE_ACTION_DATA_AVAILABLE = "com.luxshare.bluetooth.le.ACTION_DATA_AVAILABLE"
    const val BLE_ACTION_GATT_WRITE_RESULT = "com.luxshare.bluetooth.le.ACTION_WRITE_RESULT"
    const val BLE_EXTRA_DATA = "com.luxshare.bluetooth.le.EXTRA_DATA"
    const val BLE_CHARACTERISTIC = "com.luxshare.bluetooth.le.CHARACTERISTIC"
    const val BLE_ACTION_RECONNECTING = "com.luxshare.bluetooth.le.RECONNECTING"
    const val BLE_ACTION_RECONNECT_TIMEOUT = "com.luxshare.bluetooth.le.RECONNECT.TIMEOUT"

    const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    const val BLE_MTU: Int = 512
    const val BLE_HONEY_CMD_TIMEOUT: Int = 3000
    const val BLE_SERVICE_UUID = "00001910-0000-1000-8000-00805f9b34fb"
    const val BLE_NOTIFICATION_UUID = "dfd4416e-1810-47f7-8248-eb8be3dc47f9"
    const val BLE_READ_UUID = "dfd4416e-1810-47f7-8248-eb8be3dc47f9"
    const val BLE_WRITE_UUID = "9884d812-1810-4a24-94d3-b2c11a851fac"
    const val BLE_SERVICE_WIFI_CONFIG_UUID = "00008888-0000-1000-8000-00805F9B34FB"
    const val BLE_WIFI_CONFIG_UUID = "00009999-0000-1000-8000-00805F9B34FB"

    const val BATTERY_SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb"

    const val BATTERY_LEVEL_UUID = "00002a19-0000-1000-8000-00805f9b34fb"

    const val ATT_UUID_AMOTA_SERVICE = "00002760-08c2-11e1-9073-0e8ac72e1001"
    const val ATT_UUID_AMOTA_RX = "00002760-08c2-11e1-9073-0e8ac72e0001"
    const val ATT_UUID_AMOTA_TX = "00002760-08c2-11e1-9073-0e8ac72e0002"

    const val AMOTA_CMD_FW_VERIFY_RESPONSE = "01000300"
    const val AMOTA_CMD_FW_RESET_RESPONSE = "01000400"

    const val BLUETOOTH_READ_SIZE: Int = 1024
    const val BLUETOOTH_SCAN_PERIOD: Long = 15000
    const val BLUETOOTH_ACTION_GATT_CONNECTED = "com.luxshare.bluetooth.ACTION_GATT_CONNECTED"
    const val BLUETOOTH_ACTION_GATT_CONNECTING = "com.luxshare.bluetooth.ACTION_GATT_CONNECTING"
    const val BLUETOOTH_ACTION_GATT_DISCONNECTED = "com.luxshare.bluetooth.ACTION_GATT_DISCONNECTED"
    const val BLUETOOTH_ACTION_GATT_SOCKET_DISCOVERED =
        "com.luxshare.bluetooth.ACTION_GATT_socket_DISCOVERED"
    const val BLUETOOTH_ACTION_DATA_AVAILABLE = "com.luxshare.bluetooth.ACTION_DATA_AVAILABLE"


    const val BLUETOOTH_SERVICE_UUID = "00001101-0000-1000-8000-00805f9b34fb"

    const val BLUETOOTH_NOTIFICATION_UUID = "00002760-08c2-11e1-9073-0e8ac72e5402"
    const val BLUETOOTH_READ_UUID = "00002760-08c2-11e1-9073-0e8ac72e5402"
    const val BLUETOOTH_WRITE_UUID = "00002760-08c2-11e1-9073-0e8ac72e5401"
    const val BLUETOOTH_DEVICE_NAME = "luxshare-demo1"


    const val RESULT_SUCCESS = 1;
    const val RESULT_FAIL = 2;
}