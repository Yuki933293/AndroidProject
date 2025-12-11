package com.luxshare.ble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.luxshare.ble.eventbus.EventBusMessage;
import com.luxshare.ble.eventbus.message.BleMessage;
import com.luxshare.ble.eventbus.message.ConnectState;
import com.luxshare.ble.interact.ReadTaskThread;
import com.luxshare.ble.interact.WriteTaskThread;
import com.luxshare.ble.interfaces.IBluetoothDataCallBack;
import com.luxshare.ble.interfaces.IBluetoothService;
import com.luxshare.ble.interfaces.IReadListener;
import com.luxshare.ble.interfaces.IWriteListener;
import com.luxshare.ble.util.HexUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import kotlin.jvm.Synchronized;

public class BleService extends Service implements IBluetoothService {
    private static final String TAG = "BleService";
    final static int STATE_DISCONNECTED = 0;
    final static int STATE_CONNECTING = 1;
    final static int STATE_CONNECTED = 2;
    final static int STATE_SERVICES_DISCOVERED = 3;
    final static int MAX_CONNECT_COUNT = 3;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private boolean mExpectationState = false;
    private boolean mFirstReconnect = false;
    /**
     * 当前设备是否连接
     */
    private boolean isConnected = false;
    private WriteTaskThread mBluetoothWriteThread;
    private ReadTaskThread BluetoothReadThread;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Set<String> connectedAddressSet = new ArraySet<>();
    private Runnable connectRunnable = () -> {
        Log.d(TAG, "ble: connect");
        close();
        connect(TextUtils.isEmpty(mBluetoothDeviceAddress) ? "" : mBluetoothDeviceAddress);
    };
    /**
     * 用于重置设备连接状态
     */
    private Runnable resetDeviceState = new Runnable() {
        @Override
        public void run() {
            EventBus.getDefault().post(
                    new EventBusMessage(
                            EventBusMessage.MESSAGE_TYPE_BLE, new ConnectState(
                            mBluetoothDeviceAddress, false)));

        }
    };

    private Runnable reconnectingRunnable = () -> {
        Log.d(TAG, "ble: reconnecting");
        EventBus.getDefault().post(
                new EventBusMessage(
                        EventBusMessage.MESSAGE_TYPE_BLE, new BleMessage(Constants.BLE_ACTION_RECONNECTING)
                )
        );

    };
    private Runnable reconnectTimeoutRunnable = () -> {
        Log.d(TAG, "ble: reconnect timeout");
        disconnect();
        EventBus.getDefault().post(
                new EventBusMessage(
                        EventBusMessage.MESSAGE_TYPE_BLE, new BleMessage(Constants.BLE_ACTION_RECONNECT_TIMEOUT)
                )
        );
    };


    public int mConnectionState = STATE_DISCONNECTED;
    private WriteTaskThread.DataQueue<byte[]> writeQueue;

    public List<BluetoothGattService> getSupportedGattServices() {
        return mBluetoothGatt.getServices();
    }

    public WriteTaskThread.DataQueue<byte[]> getWriteQueue() {
        return writeQueue;
    }

    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getBondedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    private final ConcurrentHashMap<String,IBluetoothDataCallBack> bluetoothDataCallBacks  =
            new ConcurrentHashMap<>();

    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getConnectedDevices() {
        Set<BluetoothDevice> bondedDevices = mBluetoothAdapter.getBondedDevices();
        Set<BluetoothDevice> connectedDevices = new ArraySet<>();
        if (bondedDevices != null) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                boolean isConnect = false;
                try {
                    //获取当前连接的蓝牙信息
                    Object isConnected = bluetoothDevice.getClass().getMethod("isConnected")
                            .invoke(bluetoothDevice);
                    if (isConnected instanceof Boolean) {
                        isConnect = (boolean) isConnected;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (isConnect) {
                    connectedDevices.add(bluetoothDevice);
                }
            }
        }
        return connectedDevices;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (BluetoothGatt.GATT_SUCCESS == status) {
                Log.d(TAG, "onMtuChanged: success");
                Log.d(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                mBluetoothGatt.discoverServices();
            } else {
                Log.e(TAG, "onMtuChanged: failure");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "request MTU");
                mConnectionState = STATE_CONNECTED;
                setConnected(true);
                mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                mBluetoothGatt.requestMtu(Constants.BLE_MTU);
                EventBus.getDefault().post(
                        new EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_BLE,
                                new BleMessage(Constants.BLE_ACTION_GATT_CONNECTED)
                        )
                );
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                String address = gatt.getDevice().getAddress();
                Log.i(TAG, "onConnectionStateChange: Disconnected address:" + address);
                mHandler.removeCallbacks(resetDeviceState);
                connectedAddressSet.remove(address);
                EventBus.getDefault().post(
                        new EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_BLE, new ConnectState(
                                address, false))
                );
                setConnected(false);
                stopThread();
                mConnectionState = STATE_DISCONNECTED;
                //断开连接3秒后重连
                if (mExpectationState && mBluetoothDeviceAddress != null) {
                    //第一次重连，弹出提示，如果5秒内重连失败，则发送正在重连广播，如果1分钟内重连一直失败，则发送重连失败广播
                    if (mFirstReconnect) {
                        mFirstReconnect = false;
                        Log.d(
                                TAG, "onConnectionStateChange: ${getString(R.string.disconnect_toast)}"
                        );
                        mHandler.post(connectRunnable);
                        mHandler.postDelayed(reconnectingRunnable, 5000);
                        mHandler.postDelayed(reconnectTimeoutRunnable, 60000);
                    } else {
                        //之后每次重连，间隔3秒
                        mHandler.postDelayed(connectRunnable, 3000);
                    }
                }
                EventBus.getDefault().post(
                        new EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_BLE,
                                new BleMessage(Constants.BLE_ACTION_GATT_DISCONNECTED)
                        )
                );
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                }
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                mConnectionState = STATE_CONNECTING;
                Log.i(TAG, "Connecting from GATT server.");
                EventBus.getDefault().post(
                        new EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_BLE,
                                new BleMessage(Constants.BLE_ACTION_GATT_CONNECTING)
                        )
                );
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String address = gatt.getDevice().getAddress();
                Log.i(TAG, "onServicesDiscovered: address"+address);
                connectedAddressSet.add(address);
                mHandler.removeCallbacks(resetDeviceState);
                EventBus.getDefault().post(
                        new EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_BLE, new ConnectState(
                                address, true))
                );
                setGattServices();
                startThread();
                Log.d(TAG, "ble: connect success");
                mConnectionState = STATE_SERVICES_DISCOVERED;
                //连接上BLE，设置标志位，取消正在重连和重连失败的定时广播
                mHandler.removeCallbacks(connectRunnable);
                mHandler.removeCallbacks(reconnectingRunnable);
                mHandler.removeCallbacks(reconnectTimeoutRunnable);
                //连接成功，下次断连再连接则为第一次重连
                if (!mFirstReconnect) {
                    mFirstReconnect = true;
                    Log.d(TAG, "onConnectionStateChange: ${getString(R.string.connect_toast)}");
                }
                EventBus.getDefault().post(
                        new EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_BLE,
                                new BleMessage(Constants.BLE_ACTION_GATT_SERVICES_DISCOVERED)
                        )

                );
            } else {
                Log.w(TAG, "onServicesDiscovered received: status:" + status);
            }
        }

        /**
         * 读操作回调
         * @param gatt
         * @param characteristic
         * @param value
         * @param status
         */
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            Log.d(TAG, "onCharacteristicRead value: [" + HexUtil.formatHexString(value, ",") + "],status:" + status);
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] value = characteristic.getValue();
            Log.i(TAG, "onCharacteristicChanged: uuid:" + characteristic.getUuid()
                    + ",value:[" + HexUtil.formatHexString(value, ",") + "]");
            BleManager.getInstance().handerRecieveDatas(value);
            blueDataCallBack(value);
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status
        ) {
            EventBus.getDefault().post(new EventBusMessage(EventBusMessage.MESSAGE_TYPE_BLE,
                    new BleMessage(Constants.BLE_ACTION_GATT_WRITE_RESULT, characteristic.getUuid(), status)));
        }
    };

    private void startThread() {
        Log.d(TAG, "startThread");
        mBluetoothWriteThread = new WriteTaskThread(this, new IWriteListener() {
            @Override
            public void onWriteFailure() {
                Log.e(TAG, "onWriteFailure: ");
            }

            @Override
            public void onWriteSuccess() {
                Log.d(TAG, "onWriteSuccess: ");
            }
        });
        writeQueue = mBluetoothWriteThread.getWriteQueue();
        mBluetoothWriteThread.start();
        BluetoothReadThread = new ReadTaskThread(this, new
                IReadListener() {
                    @Override
                    public void onReadFailure() {
                        Log.e(TAG, "onReadFailure: ");
                    }

                    @Override
                    public void onReadSuccess(@NonNull byte[] readBuffer) {
                        Log.d(TAG, "onReadSuccess: readBuffer:"+ readBuffer);
                    }

                    @Override
                    public void onReadEventMessage(@NonNull EventBusMessage eventBusMessage) {
                        EventBus.getDefault().post(eventBusMessage);
                    }

                }
        );
//        BluetoothReadThread.start();
    }

    /**
     * 设置通知
     */
    private void setGattServices() {
        List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();
        for (BluetoothGattService gattService : gattServices) {
            String serviceUUID = gattService.getUuid().toString();
            BluetoothGattCharacteristic characteristic1 =
                    gattService.getCharacteristic(UUID.fromString(Constants.BLE_READ_UUID));
            Log.i(TAG, "setGattServices: characteristic1:" + characteristic1);
            if (characteristic1 != null) {
                Log.i(TAG, "setGattServices: characteristic1 serviceUUID:" + serviceUUID);
            }
            BluetoothGattCharacteristic characteristic2 =
                    gattService.getCharacteristic(UUID.fromString(Constants.BLE_WRITE_UUID));
            Log.i(TAG, "setGattServices: characteristic2:" + characteristic2);
            if (characteristic2 != null) {
                Log.i(TAG, "setGattServices: characteristic2 serviceUUID:" + serviceUUID);
            }
            if (Constants.BLE_SERVICE_UUID.equalsIgnoreCase(serviceUUID)) {
                Log.i(TAG, "get the same serviceUUID");
                BluetoothGattCharacteristic characteristic =
                        gattService.getCharacteristic(UUID.fromString(Constants.BLE_NOTIFICATION_UUID));
                Log.d(TAG, "" + Constants.BLE_NOTIFICATION_UUID);
                if (characteristic!=null){
                    setCharacteristicNotification(characteristic, true);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        // 获取通知特性的描述符
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(Constants.CLIENT_CHARACTERISTIC_CONFIG)
        );
        // 向描述符写入通知使能值
        descriptor.setValue(enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private void stopThread() {
        Log.d(TAG, "stopThread: ");
        if (mBluetoothWriteThread != null) {
            mBluetoothWriteThread.stopThread();
            mBluetoothWriteThread = null;
        }
        if (BluetoothReadThread != null) {
            BluetoothReadThread.stopThread();
            BluetoothReadThread = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private IBinder mBinder = new LocalBinder();

    class LocalBinder extends Binder {
        public BleService service = BleService.this;
    }

    @SuppressLint("MissingPermission")
    @Override
    public boolean write(byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        boolean status= false;
        try {
            BluetoothGattService gattService = mBluetoothGatt.
                    getService(UUID.fromString(Constants.BLE_SERVICE_UUID));
            if (gattService == null) {
                Log.w(TAG, "write: the gattService is null");
                return false;
            }
            BluetoothGattCharacteristic writeCharacteristic = gattService.
                    getCharacteristic(UUID.fromString(Constants.BLE_WRITE_UUID));
            writeCharacteristic.setValue(value);
            status = mBluetoothGatt.writeCharacteristic(writeCharacteristic);
            Log.i(TAG, "write: writeCharacteristic status:" + status + ",value:["
                    + HexUtil.formatHexString(value, ",") + "]");
        } catch (Exception e) {
            Log.e(TAG, "writeCharacteristic: " + e.getMessage());
            return false;
        }
        return status;
    }

    @SuppressLint("MissingPermission")
    public boolean sendConfigNetInfo(byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        boolean status= false;
        try {
            BluetoothGattService gattService = mBluetoothGatt.
                    getService(UUID.fromString(Constants.BLE_SERVICE_WIFI_CONFIG_UUID));
            if (gattService == null) {
                Log.w(TAG, "sendConfigNetInfo: the gattService is null");
                return false;
            }
            BluetoothGattCharacteristic writeCharacteristic = gattService.
                    getCharacteristic(UUID.fromString(Constants.BLE_WIFI_CONFIG_UUID));
            Log.i(TAG, "sendConfigNetInfo: writeCharacteristic:"+writeCharacteristic);
            if (writeCharacteristic != null) {
                writeCharacteristic.setValue(value);
                status = mBluetoothGatt.writeCharacteristic(writeCharacteristic);
                Log.i(TAG, "write: sendConfigNetInfo status:" + status + ",value:["
                        + HexUtil.formatHexString(value, ",") + "]");
            }
        } catch (Exception e) {
            Log.e(TAG, "writeCharacteristic: " + e.getMessage());
            return false;
        }
        return status;
    }

    @SuppressLint("MissingPermission")
    public boolean amotaWrite(byte[] value) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        boolean status = false;
        try {
            BluetoothGattCharacteristic writeCharacteristic =
                    mBluetoothGatt.getService(UUID.fromString(Constants.ATT_UUID_AMOTA_SERVICE))
                            .getCharacteristic(UUID.fromString(Constants.ATT_UUID_AMOTA_RX));
            writeCharacteristic.setValue(value);
            status = mBluetoothGatt.writeCharacteristic(writeCharacteristic);
        } catch (Exception e) {
            Log.e(TAG, "writeCharacteristic: ", e);
            return false;
        }
        return status;
    }

    @SuppressLint("MissingPermission")
    @Override
    public byte[] read() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        byte[] value = null;
        try {
            BluetoothGattService gattService = mBluetoothGatt.getService(UUID.fromString(Constants.BLE_SERVICE_UUID));
            if (gattService != null) {
                BluetoothGattCharacteristic readCharacteristic = gattService.getCharacteristic(UUID.fromString(Constants.BLE_READ_UUID));
                List<String> propersCollect = BleControlUtil.INSTANCE.getProperties(readCharacteristic.getProperties());
                Log.i(TAG, "read: propersCollect:" + String.join(",", propersCollect));
                if (mBluetoothGatt.readCharacteristic(readCharacteristic)) {
                    value = readCharacteristic.getValue();
                } else {
                    Log.i(TAG, "the readCharacteristic is false");
                }
            } else {
                Log.i(TAG, "read: the gattService is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "readCharacteristic: ", e);
            return null;
        }
        return value;
    }

    @SuppressLint("MissingPermission")
    public byte[] amotaRead() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        byte[] value = null;
        try {
            BluetoothGattCharacteristic readCharacteristic =
                    mBluetoothGatt.getService(UUID.fromString(Constants.ATT_UUID_AMOTA_TX))
                            .getCharacteristic(UUID.fromString(Constants.ATT_UUID_AMOTA_TX));
            if (mBluetoothGatt.readCharacteristic(readCharacteristic)) {
                value = readCharacteristic.getValue();
            }
        } catch (Exception e) {
            Log.e(TAG, "readCharacteristic: ", e);
            return null;
        }
        return value;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: ");
    }

    public boolean initialize() {
        if (mBluetoothManager == null) {
            Object systemService = getApplication().getSystemService(BLUETOOTH_SERVICE);
            if (systemService == null) {
                Log.e(TAG, "Unable to get BluetoothManager.");
                return false;
            } else if (systemService instanceof BluetoothManager) {
                mBluetoothManager = (android.bluetooth.BluetoothManager) systemService;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "fail to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public boolean connect(String address) {
        //连接之前先断开当前已连接的设备
        mExpectationState = false;
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        Log.i(TAG, "connect: isConnected:" + isConnected);
        if (isConnected) {
            disconnect();
        }
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            Log.e(TAG, "connect: invalid mac: $address");
            return false;
        }
        Log.i(TAG, "connect: " + address + ",mBluetoothDeviceAddress: " + mBluetoothDeviceAddress
                + ",mBluetoothGatt:" + mBluetoothGatt);
        // Previously connected device.  Try to reconnect.
        //重联很大概率不能成功；
        /*if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }*/
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        mHandler.removeCallbacks(resetDeviceState);
        mBluetoothGatt = device.connectGatt(getApplication(), false, mGattCallback);
        Log.d(TAG, "Try to create a new connection.");
        mHandler.postDelayed(resetDeviceState, 30 * 1000);
        mBluetoothDeviceAddress = address;
        return true;
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        mHandler.removeCallbacks(connectRunnable);
        mHandler.removeCallbacks(reconnectingRunnable);
        mHandler.removeCallbacks(reconnectTimeoutRunnable);
        mExpectationState = false;
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    @SuppressLint("MissingPermission")
    public byte[] getbattery() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        byte[] value = null;
        try {
            BluetoothGattCharacteristic batteryCharacteristic =
                    mBluetoothGatt.getService(UUID.fromString(Constants.BATTERY_SERVICE_UUID))
                            .getCharacteristic(UUID.fromString(Constants.BATTERY_LEVEL_UUID));
            if (mBluetoothGatt.readCharacteristic(batteryCharacteristic)) {
                value = batteryCharacteristic.getValue();
            }
        } catch (Exception e) {
            Log.e(TAG, "readCharacteristic: ", e);
            return null;
        }
        return value;
    }

    public void setFirstReconnect(boolean value) {
        mFirstReconnect = value;
    }

    public void startOtaNotify() {
        List<BluetoothGattService> gattServices = getSupportedGattServices();
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String serviceUUID = gattService.getUuid().toString();

            if (Constants.ATT_UUID_AMOTA_SERVICE == serviceUUID) {
                Log.i(TAG, "Ambiq OTA Service found $serviceUUID");
                BluetoothGattCharacteristic characteristic =
                        gattService.getCharacteristic(UUID.fromString(Constants.ATT_UUID_AMOTA_TX));
                Log.d(TAG, "setGattServices: notify mode enable, mAmotaTxChar characteristicUUID is :" + Constants.ATT_UUID_AMOTA_TX
                );
                setCharacteristicNotification(characteristic, true);
            }
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    public void endOtaNotify() {
        List<BluetoothGattService> gattServices = getSupportedGattServices();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String serviceUUID = gattService.getUuid().toString();
            if (Constants.ATT_UUID_AMOTA_SERVICE == serviceUUID) {
                Log.i(TAG, "Ambiq OTA Service found $serviceUUID");
                BluetoothGattCharacteristic characteristic =
                        gattService.getCharacteristic(UUID.fromString(Constants.ATT_UUID_AMOTA_TX));
                Log.d(TAG,
                        "setGattServices: notify mode disable, mAmotaTxChar characteristicUUID is " + Constants.ATT_UUID_AMOTA_TX
                );
                setCharacteristicNotification(characteristic, false);
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public Set<String> getConnectedAddressSet() {
        return connectedAddressSet;
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public void addBluetoothDataCallBack(String tag, IBluetoothDataCallBack bluetoothDataCallBack) {
        bluetoothDataCallBacks.put(tag, bluetoothDataCallBack);
    }

    public void removeBluetoothDataCallBack(String tag) {
        bluetoothDataCallBacks.remove(tag);
    }

    private void blueDataCallBack(byte[] readBuffer) {
        bluetoothDataCallBacks.forEach((key, value)  -> {
            value.blueDataCallBack(readBuffer);
        });
    }
}
