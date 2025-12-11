package com.luxshare.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.IBinder
import android.util.Log


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
object BleControlUtil {
    private val TAG: String = this::class.java.simpleName
    private var mBleService: BleService? = null
    private val writeQueue get() = mBleService?.writeQueue

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.
     *
     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>? get() = mBleService?.supportedGattServices

    @get:SuppressLint("MissingPermission")
    val bondedDevices: Set<BluetoothDevice>?
        get() = mBleService?.bondedDevices

    //获取当前连接的蓝牙信息
    @get:SuppressLint("MissingPermission")
    val connectedDevices: Set<BluetoothDevice>?
        get() = mBleService?.connectedDevices

    val connectionState get() = mBleService?.mConnectionState

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBleService = (service as BleService.LocalBinder).service
            if (mBleService?.initialize() == false) {
                Log.e(TAG, "Unable to initialize Bluetooth")
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBleService = null
        }
    }


    /**
     * 消息结果回调
     *
     * sbn.packageName                  包名
     * sbn.postTime                     时间
     * sbn.notification.tickerText      消息内容
     *
     */
    private val messageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {
                /*NotifyService.NOTIFY_POST -> {
                    val sbn = intent.getParcelableExtra<StatusBarNotification>(NotifyService.STATUS_BAR_NOTIFICATION)
                    Log.d(TAG, "onReceive NOTIFY_POST: $sbn")

                    if (sbn != null) {
                        if (sbn.notification == null)
                            return

                        /*//消息时间
                        val time =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(Date(sbn.postTime))
                        Log.d(TAG, "onReceive: ${String.format(
                            Locale.getDefault(),
                            "应用包名：%s\n消息内容：%s\n消息时间：%s\n",
                            sbn.packageName, msgContent, time
                        )}")*/

                        val type = when (sbn.packageName) {
                            Constants.MMS       -> 0x01
                            Constants.WX        -> 0x02
                            Constants.QQ        -> 0x03
                            else                -> 0x00
                        }

                        if (type != 0x00 && sbn.notification.tickerText != null) {
                            val name = sbn.notification.tickerText.substring(0, sbn.notification.tickerText.indexOf(": "))
                            val message = sbn.notification.tickerText.substring(sbn.notification.tickerText.indexOf(": ") + 1)

                            Log.d(TAG, "onReceive: name is $name, message os $message")
                            val notificationList = BleMessageUtil.sendNotification(
                                message.toByteArray(Charsets.UTF_8),
                                0,
                                type,
                                sbn.postTime / 1000,
                                name
                            )
                            for (notification in notificationList) {
                                writeQueue?.add(notification)
                            }
                        }
                    }
                }
                NotifyService.NOTIFY_REMOVE -> {
                    val sbn = intent.getParcelableExtra<StatusBarNotification>(NotifyService.STATUS_BAR_NOTIFICATION)
                    Log.d(TAG, "onReceive NOTIFY_REMOVE: $sbn")
                }*/
                else -> {
                    Log.d(TAG, "onReceive else: unsupported type")
                }
            }
        }
    }



    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    fun init(context: Context): Boolean {
        val gattServiceIntent = Intent(context, BleService::class.java)
        gattServiceIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.applicationContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        initReceiver(context)
        return true
    }

    private fun initReceiver(context: Context) {
        //注册广播接收
        val intentFilter = IntentFilter()

        // 通知
//        intentFilter.addAction(NotifyService.NOTIFY_POST)
        // 通知
//        intentFilter.addAction(NotifyService.NOTIFY_REMOVE)

        context.applicationContext.registerReceiver(messageReceiver, intentFilter)
    }


    fun release() {
        mBleService?.close()
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        return mBleService?.connect(address) == true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        mBleService?.disconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @SuppressLint("MissingPermission")
    fun close() {
        mBleService?.close()
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     */
    @SuppressLint("MissingPermission")
    fun read(): ByteArray? {
        return mBleService?.read()
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
        return mBleService?.write(value) == true
    }

    /**
     * prompt picture:
     * @param byteArray     表示文本内容
     * @param codeScheme    0表示UTF-8编码，1表示GBK编码，2表示GB2312编码，3表示Big5编码，4表示Unicode编码
     * @param direction: 此段文本的direction，0表示Source，1表示Destination；
     * @param language  0表示English，1表示Simplified Chinese，2表示Traditional Chinese，3 表示Japanese, 其它语言后续待增；
     */
    @Synchronized
    fun sendText(byteArray: ByteArray, codeScheme: Int, direction: Int, language: Int) {
        val textList = BleMessageUtil.sendText(byteArray, codeScheme, direction, language)
        for (text in textList) {
            writeQueue?.add(text)
        }
    }

    /**
     * prompt picture:
     * @param area          prompt显示区域，8个字节，分别是，start_x, start_y, width, height     暂不生效
     * @param pictureId     图片ID：1表示start，2表示front，4表示left，7表示right
     */
    fun sendPromptPicture(area: ByteArray, pictureId: Int) {
        writeQueue?.add(BleMessageUtil.sendPromptPicture(area, pictureId))
    }

    /**
     * prompt text:
     * @param byteArray     表示提示内容
     * @param index         0:  目的地文本,1：导航文字内容,2：剩余时间/里程文本
     * @param codeScheme    0表示UTF-8编码，1表示GBK编码，2表示GB2312编码，3表示Big5编码，4表示Unicode编码
     */
    fun sendPromptText(byteArray: ByteArray, index:Int, codeScheme: Int) {
        writeQueue?.add(BleMessageUtil.sendPromptText(byteArray, index, codeScheme))
    }

    /**
     * sync time:
     * @param time          表示秒时间戳，4个字节
     */
    fun syncTime(time: Long) {
        writeQueue?.add(BleMessageUtil.syncTime(time))
    }

    /**
     * send button:
     *
     * @param button        0x00: KEY_LEFT，0x01: KEY_RIGHT，0x02: KEY_ENTER，0x03: KEY_BACK，0x04: KEY_HOME
     */
    fun sendButton(button: Byte) {
        writeQueue?.add(BleMessageUtil.sendButton(button))
    }

    /**
     * start service:
     * @param serviceId     0x00：逐句翻译，0x10: 逐字翻译实时，0x01：导航，0x02：实时字幕，0x03：AI助手，0x0F：OTA升级
     */
    fun startService(serviceId: Byte) {
        writeQueue?.add(BleMessageUtil.startService(serviceId))
    }

    /**
     * end service:
     * @param serviceId     0x00：逐句翻译，0x10: 逐字翻译实时，0x01：导航，0x02：实时字幕，0x03：AI助手，0x0F：OTA升级
     */
    fun endService(serviceId: Byte) {
        writeQueue?.add(BleMessageUtil.endService(serviceId))
    }

    /**
     * change lighting:
     * @param lighting      1-100
     */
    fun changeLighting(lighting: Byte) {
        writeQueue?.add(BleMessageUtil.changeLighting(lighting))
    }

    /**
     * change lightingMode:
     * @param mode         0x00：关闭，0x01：开启
     */
    fun changeLightingMode(mode: Byte) {
        writeQueue?.add(BleMessageUtil.changeLightingMode(mode))
    }

    /**
     * change wearDetection:
     * @param mode         0x00：关闭，0x01：开启
     */
    fun changeWearDetection(mode: Byte) {
        writeQueue?.add(BleMessageUtil.changeWearDetection(mode))
    }

    /**
     * change language:
     * @param language         0x00：0表示English，0x01：Simplified Chinese
     */
    fun changeLanguage(language: Byte) {
        writeQueue?.add(BleMessageUtil.changeLanguage(language))
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     */
    fun amotaRead(): ByteArray? {
        return mBleService?.amotaRead()
    }

    /**
     * Request a write on a given `BluetoothGattCharacteristic`. The write result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param value The value to write.
     */
    fun amotaWrite(value: ByteArray): Boolean {
        return mBleService?.amotaWrite(value) == true
    }

    fun getbattery(): ByteArray? {
        return mBleService?.getbattery()
    }

    fun setFirstReconnect(value: Boolean) {
        mBleService?.setFirstReconnect(value)
    }

    /**
     * 表示聊天文本开始
     */
    fun startChatText(language: Int, role: Int) {
        writeQueue?.add(BleMessageUtil.startChatText(language, role))
    }

    /**
     * 自行编号，整体发送
     * 用于role i
     */
    fun sendChatText(byteArray: ByteArray, codeScheme: Int) {
        val textList = BleMessageUtil.sendChatText(byteArray, codeScheme)
        for (text in textList) {
            writeQueue?.add(text)
        }
    }


    /**
     * 指定序号，单个发送
     * 用于role ai
     */
    fun sendChatText(byteArray: ByteArray, codeScheme: Int, index: Int) {
        writeQueue?.add(BleMessageUtil.sendChatText(byteArray, codeScheme, index))
    }

    /**
     * 表示聊天文本结束
     */
    fun endChatText() {
        writeQueue?.add(BleMessageUtil.endChatText())
    }

    /**
     * 表示改变眼镜音量
     */
    fun changeVolume(volume: Int) {
        writeQueue?.add(BleMessageUtil.changeVolume(volume))
    }

    fun startOtaNotify() {
        mBleService?.startOtaNotify()
    }

    fun endOtaNotify() {
        mBleService?.endOtaNotify()
    }

    fun getProperties(property: Int): List<String> {
        val properties: MutableList<String> = ArrayList()
        for (i in 0..7) {
            when (property and (1 shl i)) {
                0x01 -> properties.add("Broadcast")
                0x02 -> properties.add("Read")
                0x04 -> properties.add("Write No Response")
                0x08 -> properties.add("Write")
                0x10 -> properties.add("Notify")
                0x20 -> properties.add("Indicate")
                0x40 -> properties.add("Authenticated Signed Writes")
                0x80 -> properties.add("Extended Properties")
            }
        }
        return properties
    }
}
