package com.luxshare.ble.bean

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult

data class DeviceInfo(
    var name: String? = "",
    var address: String = "",
    var status: Int = 0,
    var description: String = "",
    var sync: Boolean = true,
    var model: String = "",
    var serialNumber: String = "",
    var firmwareVersion: String = "",
    var hashCode: Int = 0,
    var rssi: Int = 0,
    var type: Int = 0,
    var bluetoothAddress: String = "",
) {

    companion object {
        @SuppressLint("MissingPermission")
        fun getDeviceInfo(scanResult: ScanResult): DeviceInfo {
            val deviceInfo = DeviceInfo()
            deviceInfo.rssi = scanResult.rssi
            val bluetoothDevice = scanResult.device
            deviceInfo.name = bluetoothDevice.name
            deviceInfo.address = bluetoothDevice.address
            deviceInfo.type = bluetoothDevice.type
            return deviceInfo
        }
    }

}