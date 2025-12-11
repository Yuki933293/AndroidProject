package com.luxshare.bluetooth.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

import com.luxshare.bluetooth.BluetoothService
import com.luxshare.bluetooth.listener.IBluetoothDataCallBack
import com.luxshare.bluetooth.listener.IBluetoothSppStateListener

public object BluetoothControlUtil {
    private val TAG: String = "BluetoothControlUtil"
    private var mBluetoothService: BluetoothService? = null
    private val writeQueue get() = mBluetoothService?.writeQueue

    @get:SuppressLint("MissingPermission")
    val bondedDevices: Set<BluetoothDevice>?
        get() = mBluetoothService?.bondedDevices

    //获取当前连接的蓝牙信息
    @get:SuppressLint("MissingPermission")
    val connectedDevices: Set<BluetoothDevice>?
        get() = mBluetoothService?.connectedDevices

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothService = (service as BluetoothService.LocalBinder).service
            Log.i(TAG, "service connect success")
            if (mBluetoothService?.initialize() == false) {
                 Log.e(TAG, "Unable to initialize Bluetooth")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.i(TAG, "service connect fail")
            mBluetoothService = null
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public fun init(context: Context): Boolean {
        val gattServiceIntent = Intent(context, BluetoothService::class.java)
        gattServiceIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.applicationContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        return true
    }

    fun release() {
        mBluetoothService?.close()
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        return mBluetoothService?.connect(address) == true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        mBluetoothService?.disconnect()
    }

    @SuppressLint("MissingPermission")
    fun reconnect() {
        mBluetoothService?.reconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @SuppressLint("MissingPermission")
    fun close() {
        mBluetoothService?.close()
    }

    /**
     * 如果设备已配对但未连接，则需要跳转系统界面手动连接
     *
     */
    fun isBonded(address: String): Boolean {
        return mBluetoothService?.isBonded(address) == true
    }

    /**
     * 如果设备已配对但未连接，则需要跳转系统界面手动连接
     *
     */
    fun isConnected(address: String): Boolean {
        return mBluetoothService?.isConnected(address) == true
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     */
    @SuppressLint("MissingPermission")
    fun read(): ByteArray? {
        return mBluetoothService?.read()
    }

    /**
     * Request a write on a given `BluetoothGattCharacteristic`. The write result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param value The value to write.
     */
    @SuppressLint("MissingPermission")
    fun write(value: ByteArray): Boolean {
        return mBluetoothService?.write(value) == true
    }

    fun getBlueDevice(mac:String): BluetoothDevice? {
        return mBluetoothService?.getBlueDevice(mac)
    }

    @Synchronized
    fun sendBlueData(byteArray: ByteArray) {
        if (byteArray.isEmpty()) {
            Log.i(TAG, "send data is null")
            return
        }
        writeQueue?.add(byteArray)
    }

    fun addBluetoothDataCallBack(tag:String, bluetoothDataCallBack: IBluetoothDataCallBack) {
        mBluetoothService?.addBluetoothDataCallBack(tag, bluetoothDataCallBack)
    }

    fun removeBluetoothDataCallBack(tag:String) {
        mBluetoothService?.removeBluetoothDataCallBack(tag)
    }

    fun addBluetoothStateListener(tag: String, bluetoothStateListener: IBluetoothSppStateListener) {
        mBluetoothService?.addBluetoothStateListener(tag, bluetoothStateListener)
    }

    fun removeStatusListener(tag: String) {
        mBluetoothService?.removeStatusListener(tag)
    }
}