package com.luxshare.bluetooth

interface IBluetoothService {
    fun write(value: ByteArray): Boolean
    fun read(): ByteArray?
}