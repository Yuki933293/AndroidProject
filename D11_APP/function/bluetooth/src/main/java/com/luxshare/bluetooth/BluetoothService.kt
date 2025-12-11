/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luxshare.bluetooth

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.ArraySet
import android.util.Log
import com.luxshare.base.utils.LuxExecutor
import com.luxshare.bluetooth.listener.*
import com.luxshare.bluetooth.constants.Constants
import com.luxshare.bluetooth.thread.*
import com.luxshare.bluetooth.util.BlueConnectStateUtil
import com.luxshare.bluetooth.util.BluetoothReflectUtils
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing connection and data comunication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
class BluetoothService : Service(), IBluetoothService {
    companion object {
        val TAG: String = "BluetoothService"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Constants.BLUETOOTH_ACTION_GATT_CONNECTED)
            intentFilter.addAction(Constants.BLUETOOTH_ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(Constants.BLUETOOTH_ACTION_GATT_SOCKET_DISCOVERED)
            intentFilter.addAction(Constants.BLUETOOTH_ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mConnectBtThread: ConnectThread? = null
    private var mSocket: BluetoothSocket? = null
    private val mReadBuffer = ByteArray(Constants.BLUETOOTH_READ_SIZE) //buffer store for the stream

    private var mBluetoothWriteThread: BluetoothWriteThread? = null
    private var mBluetoothReadThread: BluetoothReadThread? = null

    private val connectedMacs: MutableSet<String> by lazy {
        ArraySet()
    }

    val writeQueue: WriteQueue<ByteArray> = WriteQueue()

    // 解决线程安全
    private val bluetoothStateListeners: ConcurrentHashMap<String, IBluetoothSppStateListener> by lazy {
        ConcurrentHashMap()
    }

    // 解决线程安全
    private val bluetoothDataCallBacks : ConcurrentHashMap<String,IBluetoothDataCallBack> by lazy {
        ConcurrentHashMap()
    }

    private val blueConnectStateUtil: BlueConnectStateUtil by lazy {
        BlueConnectStateUtil(this, bluetoothStateListener)
    }

    @get:SuppressLint("MissingPermission")
    val bondedDevices: Set<BluetoothDevice>
        get() = mBluetoothAdapter?.bondedDevices as Set<BluetoothDevice>


    //获取当前连接的蓝牙信息
    @get:SuppressLint("MissingPermission")
    val connectedDevices: Set<BluetoothDevice>
        get() {
            val bondedDevices = mBluetoothAdapter?.bondedDevices
            val connectedDevices: MutableSet<BluetoothDevice> = ArraySet()
            if (bondedDevices != null) {
                for (bluetoothDevice in bondedDevices) {
                    var isConnect = false
                    try {
                        //获取当前连接的蓝牙信息
                        isConnect = bluetoothDevice::class.java.getMethod("isConnected")
                            .invoke(bluetoothDevice) as Boolean
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: InvocationTargetException) {
                        e.printStackTrace()
                    } catch (e: NoSuchMethodException) {
                        e.printStackTrace()
                    }
                    if (isConnect) {
                        connectedDevices.add(bluetoothDevice)
                    }
                }
            }
            return connectedDevices
        }

    private val bluetoothStateListener: IBluetoothDeviceStateListener = object :
        IBluetoothDeviceStateListener {
        override fun onBluetoothSPPBondStateChanged(device: BluetoothDevice?) {
            Log.d(TAG, "onBluetoothSPPBondStateChanged ${device?.address}")
        }

        override fun onBluetoothConnected(device: BluetoothDevice?) {
            Log.d(TAG, "onBluetoothConnected ${device?.address}")
        }

        override fun onBluetoothDisconnected(device: BluetoothDevice?) {
            Log.d(TAG, " onBluetoothDisconnected ${device?.address}")
            stopThread()
            connectedMacs.remove(device?.address)
        }

    }

    //蓝牙结果回调
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "onReceive:${intent.action}")
            when (intent.action) {
                Constants.BLUETOOTH_ACTION_GATT_CONNECTED -> {
                    startThread()
                }
                Constants.BLUETOOTH_ACTION_GATT_DISCONNECTED -> {
                    stopThread()
                }
                Constants.BLUETOOTH_ACTION_GATT_CONNECTING -> {

                }
                Constants.BLUETOOTH_ACTION_GATT_SOCKET_DISCOVERED -> {

                }
                Constants.BLUETOOTH_ACTION_DATA_AVAILABLE -> {

                }
                else -> {

                }
            }
        }
    }

    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: BluetoothService
            get() = this@BluetoothService
    }

    override fun onBind(intent: Intent): IBinder {
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        blueConnectStateUtil.registerBlueStateReceiver()
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        unregisterReceiver(mGattUpdateReceiver)
        blueConnectStateUtil.unregisterBlueStateReceiver()
        close()
        return super.onUnbind(intent)
    }

    /**
     * 执行绑定 反射
     * @param bluetoothDevice 蓝牙设备
     * @return true 执行绑定 false 未执行绑定
     */
    fun bondDevice(bluetoothDevice: BluetoothDevice?): Boolean {
        if (bluetoothDevice == null) {
            Log.e(TAG, "boundDevice-->bluetoothDevice == null")
            return false
        }
        try {
            return BluetoothReflectUtils.createBond(BluetoothDevice::class.java, bluetoothDevice)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return true
    }

    /**
     * 执行解绑  反射
     * @param bluetoothDevice 蓝牙设备
     * @return  true 执行解绑  false未执行解绑
     */
    fun removeBond(bluetoothDevice: BluetoothDevice?): Boolean {
        if (bluetoothDevice == null) {
            Log.e(TAG, "disBoundDevice-->bluetoothDevice == null")
            return false
        }
        try {
            return BluetoothReflectUtils.removeBond(BluetoothDevice::class.java, bluetoothDevice)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return true
    }

    fun getBlueDevice(mac: String):BluetoothDevice? {
        return if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
            Log.e(TAG, "getBlueDevice: invalid mac: $mac")
            null
        } else {
            mBluetoothAdapter?.getRemoteDevice(mac)
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }
        mBluetoothAdapter = mBluetoothManager?.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }


    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.e(TAG, "getBlueDevice: invalid mac: $address")
            return false
        }
        if (connectedMacs.contains(address)) {
            Log.i(TAG, "已经连接上，无需重复连接")
            sppSuccessCallBack()
            return true
        }
        if (mConnectBtThread != null) {
            mConnectBtThread?.cancel()
            mConnectBtThread = null
        }

        mConnectBtThread = ConnectThread(address)
        mConnectBtThread?.start()
        return true
    }

    /**
     * 断开已有的连接
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        mConnectBtThread?.cancel()
        mConnectBtThread = null
    }

    private fun startThread() {
        Log.d(TAG, "startThread: ")
        mBluetoothWriteThread = BluetoothWriteThread(writeQueue, this, object :
            IWriteListener {
            override fun onWriteSuccess() {
                 Log.d(TAG, "onWriteSuccess: ")
            }

            override fun onWriteFailure() {
                Log.e(TAG, "onWriteFailure: ")
            }
        })
        mBluetoothWriteThread?.start() // 启动向Bluetooth写入数据线程

        mBluetoothReadThread = BluetoothReadThread(this, object :
            IReadListener {
            override fun onReadSuccess(readBuffer: ByteArray?) {
                readBuffer?.let {
                    if (it.isNotEmpty()) {
                        blueDataCallBack(it)
                    }
                }
            }

            override fun onReadFailure() {

            }
        })
        mBluetoothReadThread?.start() // 启动向Bluetooth读数据线程
    }

    private fun stopThread() {
        Log.d(TAG, "stopThread: ")
        mBluetoothWriteThread?.stopThread() // 停止向Ble写入数据线程
        mBluetoothWriteThread = null
        mBluetoothReadThread?.stopThread() // 停止向Ble写入数据线程
        mBluetoothReadThread = null
    }

    @SuppressLint("MissingPermission")
    fun reconnect() {
//        for (device in connectedDevices) {
//            if (device.name != null && device.name.contains(Constants.BLUETOOTH_DEVICE_NAME)) {
//                val result = connect(device.address)
//                Log.d(TAG, "Connect request result=$result")
//
//                return
//            }
//        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    /**
     * After using a given BLUETOOTH device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        try {
            mSocket?.close()
            mSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "close: ", e)
        }
    }

    /**
     * 判断设备是否已配对
     *
     */
    fun isBonded(address: String): Boolean {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.e(TAG, "isBonded: invalid mac: $address")
            return false
        }

        var bonded = false

        for (device in bondedDevices) {
            if (address == device.address) {
                bonded = true
                break
            }
        }

        return bonded
    }

    /**
     * 判断设备是否已连接
     *
     */
    fun isConnected(address: String): Boolean {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.e(TAG, "isConnected: invalid mac: $address")
            return false
        }

        var connected = false

        for (device in connectedDevices) {
            if (address == device.address) {
                connected = true
                break
            }
        }

        return connected
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     */
    @SuppressLint("MissingPermission")
    override fun read(): ByteArray? {
        if (mBluetoothAdapter == null || mSocket == null) {
            Log.w(TAG, "read BluetoothAdapter not initialized")
            stopThread()
            return null
        }
        var count = 0
        try {
            val available: Int = mSocket?.inputStream?.available() ?: 0
            if (available > 0) {
                //从输入流中读取的一定数量字节数,并将它们存储到缓冲区buffer数组中，count为实际读取的字节数
                count = mSocket?.inputStream?.read(mReadBuffer) ?: 0
            }
        }catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "读取异常:${e.message}")
        }
        //返回实际读取的数据内容
        return mReadBuffer.copyOf(count)
    }

    /**
     * Request a write on a given `BluetoothGattCharacteristic`. The write result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param value The value to write.
     */
    @SuppressLint("MissingPermission")
    override fun write(value: ByteArray): Boolean {
        if (mBluetoothAdapter == null || mSocket == null) {
            stopThread()
            Log.w(TAG, "write BluetoothAdapter not initialized")
            return false
        }

        try {
            //发送数据
            mSocket?.outputStream?.write(value)
        } catch (e: IOException) {
            Log.e(TAG, "write: ", e)
            return false
        }
        return true
    }

    inner class ConnectThread(mac: String) : Thread() {
        var address = ""
        var bluetoothDevice: BluetoothDevice? = null

        init {
            address = mac
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            Log.d(TAG, "run: connect bluetooth mac is $address")

            if (!BluetoothAdapter.checkBluetoothAddress(address)) {
                Log.e(TAG, "connect: invalid mac: $address")
                sppFailedCallBack()
                return
            }

            if (mSocket != null) {
                try {
                    mSocket?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "run: ", e)
                    sppFailedCallBack()
                    return
                }
            }

            bluetoothDevice = mBluetoothAdapter?.getRemoteDevice(address)

            //连接之前先取消发现设备，否则会大幅降低连接尝试的速度，并增加连接失败的可能性
            if (mBluetoothAdapter?.isDiscovering == true) {
                mBluetoothAdapter?.cancelDiscovery()
            }

            // 连接建立之前的先配对
            if (bluetoothDevice?.bondState == BluetoothDevice.BOND_NONE) {
                if (bluetoothDevice?.createBond() != true) {
                    Log.e(TAG, "createBond: failure")
                    sppFailedCallBack()
                    return
                }
            }

            //1、获取BluetoothSocket
            try {
                //建立安全的蓝牙连接，会弹出配对框
               mSocket = BluetoothReflectUtils.cretateBluetoothSocketbyChannel(bluetoothDevice, 29)
//                mSocket =
//                    bluetoothDevice?.createRfcommSocketToServiceRecord(UUID.fromString(Constants.BLUETOOTH_SERVICE_UUID)) //加密传输，Android系统强制配对，弹窗显示配对码

            } catch (e: IOException) {
                Log.e(TAG, "connect: ", e)
                sppFailedCallBack()
                return
            }
            if (mSocket == null) {
                Log.e(TAG, "ConnectThread:run-->mSocket == null")
                sppFailedCallBack()
                return
            }

            //2、通过mSocket去连接设备
            try {
                Log.d(TAG, "start connect")
                //connect()为阻塞调用，连接失败或 connect() 方法超时（大约 12 秒之后），它将会引发异常
                mSocket?.connect()
                Log.d(TAG, "connect success")
                sppSuccessCallBack()
                connectedMacs.add(address)
                startThread()
            } catch (e: IOException) {
                Log.e(TAG, "连接异常:", e)
                cancel()
                stopThread()
                sppFailedCallBack()
            }
        }

        fun cancel() {
            try {
                //关闭输入流
                mSocket?.inputStream?.close()
                //关闭输出流
                mSocket?.outputStream?.close()
                //关闭mSocket
                mSocket?.close()
            } catch (e: IOException) {
                // 任何一部分报错，都将强制关闭mSocket连接
                Log.e(TAG, "disconnect: ", e)
            } finally {
                mSocket = null
            }
        }
    }

    fun addBluetoothDataCallBack(tag: String, bluetoothDataCallBack: IBluetoothDataCallBack) {
        bluetoothDataCallBacks[tag] = bluetoothDataCallBack
    }

    fun removeBluetoothDataCallBack(tag: String) {
        bluetoothDataCallBacks.remove(tag)
    }

    fun addBluetoothStateListener(tag: String, bluetoothStateListener: IBluetoothSppStateListener) {
        bluetoothStateListeners[tag] = bluetoothStateListener
    }

    fun removeStatusListener(tag: String) {
        bluetoothStateListeners.remove(tag)
    }

    @Synchronized
    private fun blueDataCallBack(readBuffer: ByteArray) {
        LuxExecutor.runOnUiThread {
            bluetoothDataCallBacks.forEach {
                it.value.blueDataCallBack(readBuffer)
            }
        }
    }

    @Synchronized
    private fun sppFailedCallBack() {
        LuxExecutor.runOnUiThread {
            bluetoothStateListeners.forEach {
                it.value.onBluetoothSPPDisConnected()
            }
        }
    }

    @Synchronized
    private fun sppSuccessCallBack() {
        LuxExecutor.runOnUiThread {
            bluetoothStateListeners.forEach {
                it.value.onBluetoothSPPConnected()
            }
        }
    }
}