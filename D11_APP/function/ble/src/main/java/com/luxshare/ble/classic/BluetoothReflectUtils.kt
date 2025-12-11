package com.luxshare.common.bluetooth.classic

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.lang.reflect.InvocationTargetException


object BluetoothReflectUtils {
    private val TAG = this::class.java.simpleName
    var remoteDevice: BluetoothDevice? = null

    /**
     * 与设备配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    @Throws(Exception::class)
    fun createBond(btClass: Class<*>, btDevice: BluetoothDevice?): Boolean {
        val createBondMethod = btClass.getMethod("createBond")
        return createBondMethod.invoke(btDevice) as Boolean
    }

    //自动配对设置Pin值
    @Throws(Exception::class)
    fun autoBond(btClass: Class<*>, device: BluetoothDevice?, strPin: String): Boolean {
        val autoBondMethod = btClass.getMethod(
            "setPin", *arrayOf<Class<*>>(
                ByteArray::class.java
            )
        )
        return autoBondMethod.invoke(device, strPin.toByteArray()) as Boolean
    }

    @SuppressLint("SoonBlockedPrivateApi")
    fun setPairingConfirmation(device: BluetoothDevice) {
        try {
            val field = device::class.java.getDeclaredField("sService")
            field.isAccessible = true
            val service = field[device]
            val method = service::class.java.getDeclaredMethod(
                "setPairingConfirmation",
                BluetoothDevice::class.java, Boolean::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(service, device, true)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    @Throws(Exception::class)
    fun removeBond(btClass: Class<*>, btDevice: BluetoothDevice?): Boolean {
        val removeBondMethod = btClass.getMethod("removeBond")
        return removeBondMethod.invoke(btDevice) as Boolean
    }

    @Throws(Exception::class)
    fun setPin(
        btClass: Class<*>, btDevice: BluetoothDevice?,
        str: String,
    ): Boolean {
        try {
            val removeBondMethod = btClass.getDeclaredMethod(
                "setPin", *arrayOf<Class<*>>(
                    ByteArray::class.java
                )
            )
            val returnValue = removeBondMethod.invoke(
                btDevice,
                *arrayOf<Any>(str.toByteArray())
            ) as Boolean
        } catch (e: SecurityException) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace()
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return true
    }

    // 取消用户输入
    //cancelPairingUserInput（）取消用户输入密钥框，
    // 个人觉得一般情况下不要和
    // setPin（setPasskey、setPairingConfirmation、 setRemoteOutOfBandData）一起用，
    // 这几个方法都会remove掉map里面的key:value（也就是互斥的 ）
    @Throws(Exception::class)
    fun cancelPairingUserInput(
        btClass: Class<*>,
        device: BluetoothDevice?,
    ): Boolean {
        val createBondMethod = btClass.getMethod("cancelPairingUserInput")
        // cancelBondProcess()
        return createBondMethod.invoke(device) as Boolean
    }

    // 取消配对
    @Throws(Exception::class)
    fun cancelBondProcess(
        btClass: Class<*>,
        device: BluetoothDevice?,
    ): Boolean {
        val createBondMethod = btClass.getMethod("cancelBondProcess")
        return createBondMethod.invoke(device) as Boolean
    }

    /**
     * @param clsShow
     */
    fun printAllInform(clsShow: Class<*>) {
        try {
            // 取得所有方法
            val hideMethod = clsShow.methods
            var i = 0
            while (i < hideMethod.size) {
                i++
            }
            // 取得所有常量
            val allFields = clsShow.fields
            i = 0
            while (i < allFields.size) {
                i++
            }
        } catch (e: SecurityException) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace()
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    /**
     * Clears the internal cache and forces a refresh of the services from the	 * remote device.
     */
    fun refreshDeviceCache(mBluetoothGatt: BluetoothGatt?): Boolean {
        if (mBluetoothGatt != null) {
            try {
                val localBluetoothGatt: BluetoothGatt = mBluetoothGatt
                val localMethod =
                    localBluetoothGatt::class.java.getMethod("refresh", *arrayOfNulls(0))
                return (localMethod.invoke(localBluetoothGatt, *arrayOfNulls(0)) as Boolean)
            } catch (localException: Exception) {
                Log.i("Config", "An exception occured while refreshing device")
            }
        }
        return false
    }


    @SuppressLint("MissingPermission")
    fun cretateBluetoothSocketbyChannel(device: BluetoothDevice?, channel: Int): BluetoothSocket? {
        var socket: BluetoothSocket? = null
        if (device != null) {
            try {
                Log.d(TAG, "Trying fallback on channel $channel")
                socket = device::class.java.getMethod(
                    "createRfcommSocket",
                    *arrayOf<Class<*>?>(Int::class.javaPrimitiveType)
                ).invoke(device, channel) as BluetoothSocket
                Log.d(TAG, "createRfcommSocket on channel $channel")
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "cretateBluetoothSocketbyChannel: ", e)
            }
        }

        return socket
    }
}