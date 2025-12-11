package com.luxshare.ble.classic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.luxshare.ble.Constants

class BluetoothScanUtil (context: Context) {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning = false
    private var mHandler: Handler? = null

    init {
        mHandler = Handler(Looper.getMainLooper())

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        mScanning = false
        mBluetoothAdapter?.cancelDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (mBluetoothAdapter?.isDiscovering == true) {
            return
        }

        mHandler?.postDelayed({
            mScanning = false
            mBluetoothAdapter?.cancelDiscovery()
        }, Constants.BLUETOOTH_SCAN_PERIOD)
        //开始搜索
        mScanning = true
        mBluetoothAdapter?.startDiscovery()
    }
}