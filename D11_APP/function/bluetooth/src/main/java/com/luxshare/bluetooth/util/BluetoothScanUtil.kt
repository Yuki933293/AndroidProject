package com.luxshare.bluetooth.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.luxshare.bluetooth.listener.IBluetoothScanCallBack

class BluetoothScanUtil (val context: Context, val scanCallback: IBluetoothScanCallBack?) {
    private val TAG = "BluetoothScanUtil"
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning = false
    private var mHandler: Handler? = null

    val bondedDevices: Set<BluetoothDevice>
        @SuppressLint("MissingPermission")
        get() = mBluetoothAdapter?.bondedDevices as Set<BluetoothDevice>

    init {
        mHandler = Handler(Looper.getMainLooper())

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            Log.i(TAG, "onReceive:${intent.action}")
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // 找到一个蓝牙设备
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // 处理设备
                device?.let {
                    scanCallback?.callBack(it)
                }
            } else if (action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                scanCallback?.callBackEnd()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        mScanning = false
        mBluetoothAdapter?.cancelDiscovery()
        context.unregisterReceiver(bluetoothReceiver)
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (mBluetoothAdapter?.isDiscovering == true) {
            return
        }
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)

//        mHandler?.postDelayed({
//            mScanning = false
//            mBluetoothAdapter?.cancelDiscovery()
//            scanCallback?.callBackEnd()
//        }, Constants.BLUETOOTH_SCAN_PERIOD)
        //开始搜索
        mScanning = true
        mBluetoothAdapter?.startDiscovery()
    }
}