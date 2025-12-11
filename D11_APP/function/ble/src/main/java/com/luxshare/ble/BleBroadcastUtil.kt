package com.luxshare.ble

import com.luxshare.ble.util.HexUtil

object BleBroadcastUtil {
    fun parseData(byteArray: ByteArray): HashMap<Byte, ByteArray> {
        val dataMap = hashMapOf<Byte, ByteArray>()
        var index = 0
        while (index < byteArray.size) {
            if (byteArray[index] == 0x0.toByte()) {
                break
            }

            val len = byteArray[index]
            val type = byteArray[index + 1]
            val value = byteArray.copyOfRange(index + 2, index + len + 1)
            dataMap[type] = value
            index += len + 1
        }
        return dataMap
    }

    fun formatMac(byteArray: ByteArray): String {
        return HexUtil.formatHexString(byteArray, ":").uppercase()
    }
}