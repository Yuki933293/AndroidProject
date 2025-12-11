package com.luxshare.bluetooth.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.luxshare.bluetooth.listener.IBluetoothDeviceStateListener

/**
 * @desc 功能描述
 * @author hudebo
 * @date 2023/12/11
 */
class BlueConnectStateUtil(private val context: Context, private val stateListener: IBluetoothDeviceStateListener?) {
    private val TAG = "BlueConnectStateUtil"

    /**
     * 蓝牙广播接收器
     */
    private val bluetoothBroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    //获取蓝牙广播中的蓝牙连接新状态
                    val newConnState =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, 0)

                    Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED connect state: $newConnState")
                }

                // 有远程设备成功连接至本机
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    // 当前远程蓝牙设备
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    Log.d(TAG, "onReceive ACTION_ACL_CONNECTED: ${device?.address}")
                    stateListener?.onBluetoothConnected(device)
                }
                // 有远程设备断开连接
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    // 当前远程蓝牙设备
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    Log.d(TAG, "onReceive ACTION_ACL_DISCONNECTED: ${device?.address}")
                    stateListener?.onBluetoothDisconnected(device)
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    Log.d(TAG, "onReceive: bond state changed")
                    val bluetoothDevice =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    stateListener?.onBluetoothSPPBondStateChanged(bluetoothDevice)
                }
            }
        }
    }


    /**
     * 初始化蓝牙
     */
    fun registerBlueStateReceiver() {
        //注册广播接收
        val intentFilter = IntentFilter()

        // 蓝牙开关状态
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        // 本机的蓝牙连接状态发生变化（连接第一个远程设备与断开最后一个远程设备才触发）
        intentFilter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        // 有远程设备成功连接至本机(每个远程设备都会触发)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        // 有远程设备断开连接(每个远程设备都会触发)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        // 配对状态监听
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)

        context.registerReceiver(bluetoothBroadcastReceiver, intentFilter)
    }

    fun unregisterBlueStateReceiver() {
        context.unregisterReceiver(bluetoothBroadcastReceiver)
    }
}