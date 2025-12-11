package com.luxshare.ble;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.luxshare.base.bean.BleTLVData;
import com.luxshare.base.utils.BleCMDUtil;
import com.luxshare.base.utils.StringUtil;
import com.luxshare.base.utils.VerificationURLUtil;
import com.luxshare.ble.eventbus.EventBusMessage;
import com.luxshare.ble.eventbus.message.OtaMessage;
import com.luxshare.ble.eventbus.message.ResultMessage;
import com.luxshare.ble.interact.WriteTaskThread;
import com.luxshare.ble.interfaces.IBluetoothDataCallBack;
import com.luxshare.ble.util.HexUtil;
import com.luxshare.configs.Configs;
import com.luxshare.configs.ItemCommandManger;
import com.luxshare.fastsp.FastSharedPreferences;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

public class BleManager {
    private static BleManager instance;
    private static final String TAG = "BleManager";
    private final Handler shandler;
    private final Handler parserHandler;
    private BleService mBleService;
    private WriteTaskThread.DataQueue<byte[]> writeQueue;
    private Context sContext;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBleService = ((BleService.LocalBinder) service).service;
            if (!mBleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            } else {
                Log.d(TAG, "Ble Service init successfull");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleService = null;
        }
    };


    private BleManager() {
        HandlerThread handlerThread = new HandlerThread("tt");
        handlerThread.start();
        shandler = new Handler(handlerThread.getLooper());
        HandlerThread parserThread = new HandlerThread("parser");
        parserThread.start();
        parserHandler = new Handler(parserThread.getLooper());
    }

    public static BleManager getInstance() {
        if (instance == null) {
            instance = new BleManager();
        }
        return instance;
    }

    public boolean init(Context context) {
        Log.i(TAG, "init: ");
        this.sContext = context;
        Intent intent = new Intent(context, BleService.class);
        context.getApplicationContext().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        return true;
    }

    @SuppressLint("MissingPermission")
    public boolean connect(String address) {
        try {
            if (mBleService != null) {
                return mBleService.connect(address);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e(TAG, "connect: BleService is null");
        return false;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBleService != null) {
            mBleService.disconnect();
        }
        Log.e(TAG, "disconnect");
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @SuppressLint("MissingPermission")
    public void close() {
        if (mBleService != null) {
            mBleService.close();
        }
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     */
    @SuppressLint("MissingPermission")
    public byte[] read() {
        if (mBleService != null) {
            return mBleService.read();
        }
        Log.e(TAG, "read: BleService is null");
        return null;
    }

    /**
     * Request a write on a given `BluetoothGattCharacteristic`. The write result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.
     *
     * @param value The value to write.
     */
    @SuppressLint("MissingPermission")
    public boolean write(byte[] value) {
        Log.d(TAG, "write: value:" + value);
        if (mBleService != null) {
            return mBleService.write(value);
        }
        Log.e(TAG, "write: BleService is null");
        return false;
    }

    @SuppressLint("MissingPermission")
    public boolean setConfigedWifi(String config) {
        Log.d(TAG, "setConfigedWifi: config:" + config);
        if (mBleService != null) {
            return mBleService.sendConfigNetInfo(config.getBytes(StandardCharsets.UTF_8));
        }
        Log.e(TAG, "setConfigedWifi: BleService is null");
        return false;
    }
    /**
     * 查询电池电量
     */
    private Runnable queryBatteryTask = new Runnable() {
        private final byte[] sendCmd = ItemCommandManger.of().getSendCmd((byte) 0x11,
                1);

        @Override
        public void run() {
            shandler.removeCallbacks(queryBatteryTask);
            Log.i(TAG, "querry battery level");
            add(sendCmd);
            //音箱设备测有电量变化时会主动上报，暂时先查询一次
            //shandler.postDelayed(queryBatteryTask, Configs.QUERY_BATTERY_LEVEL_PERIOD);
        }
    };

    /**
     * 开始查询电池电量
     */
    public void startQueryBatteryLevel() {
        Log.d(TAG, "startQueryBatteryLevel");
        if (shandler != null) {
            shandler.removeCallbacks(queryBatteryTask);
            shandler.postDelayed(queryBatteryTask, 1000);
        }
    }

    /**
     * 停止查询电池电量
     */
    public void stopQueryBatteryLevel() {
        Log.d(TAG, "stopQueryBatteryLevel: ");
        if (shandler != null) {
            shandler.removeCallbacks(queryBatteryTask);
        }
    }

    /**
     * 放入写入队列发出
     *
     * @param value
     */
    public void add(byte[] value) {
        boolean isConnected = isConnected();
        Log.i(TAG, "add: isconnected:" + isConnected);
        if (!isConnected) {
            Log.e(TAG, "the device is diconnect");
            Toast.makeText(sContext, sContext.getString(R.string.disconnect_warning), Toast.LENGTH_SHORT).show();
            return;
        }
        if (mBleService.getWriteQueue() != null) {
            mBleService.getWriteQueue().add(value);
        } else {
            Log.i(TAG, "add: writeQueue is null");
        }
    }

    public void release() {
        if (mBleService != null) {
            mBleService.close();
        }
    }

    public Set<String> getConnectedAddresses() {
        if (mBleService != null) {
            return mBleService.getConnectedAddressSet();
        }
        Log.i(TAG, "getConnectedAddresses:  empty");
        return null;
    }

    /**
     * 解析数据并发送
     *
     * @param readBuffer
     */
    public void handerRecieveDatas(byte[] readBuffer) {
        parserHandler.post(() -> read2Transform(readBuffer));
    }

    private void read2Transform(byte[] readBuffer) {
        if (readBuffer == null) {
            Log.e(TAG, "decode: the buffer is null");
            return;
        }
        Log.d(TAG, "read2Transform readBuffer：[" + HexUtil.formatHexString(readBuffer, ",") + "]");
        if (readBuffer.length < 4) {
            Log.e(TAG, "decode: the buffer is error");
            return;
        }
        Log.i(TAG, "the data meets the requirement");
        if (Configs.CMD_RECIEVE_HEAD == readBuffer[0]) {
            byte group = readBuffer[2];
            byte module = readBuffer[3];
            if (module == Configs.CMD_QUERY) {
                int headLength = 4;
                int dataLength = readBuffer.length - headLength;
                if (dataLength > 0 && dataLength % 2 == 0) {
                    for (int i = 0; i < dataLength; i = i + 2) {
                        ResultMessage result = new ResultMessage();
                        result.setGroup(group);
                        result.setNicky(readBuffer[headLength + i]);
                        result.setResult(readBuffer[headLength + i + 1]);
                        postEventMessage(new EventBusMessage(EventBusMessage.MESSAGE_TYPE_BLE, result));
                    }
                } else {
                    Log.e(TAG, "decode: recieve query data is error");
                }
            } else {
                if (readBuffer.length != 5) {
                    return;
                }
                ResultMessage result = new ResultMessage();
                result.setGroup(group);
                result.setNicky(module);
                result.setResult(readBuffer[4]);
                postEventMessage(new EventBusMessage(EventBusMessage.MESSAGE_TYPE_BLE, result));
            }
        } else if (BleCMDUtil.SOF == readBuffer[0]) {
            dealOTACMDData(readBuffer);

        }
    }

    private void dealOTACMDData(byte[] readBuffer) {
        BleTLVData tlvData = BleCMDUtil.INSTANCE.parseMeetingTLVData(readBuffer);
        if (tlvData != null) {
            switch (tlvData.getOperationId()) {
                case BleCMDUtil.OTA_PATH_L: {
                    String otaPath = StringUtil.INSTANCE.byteConvertString(tlvData.getData());
                    Log.i(TAG, "ota path =" + otaPath);
                    String spLocalPath = FastSharedPreferences.get(Configs.OTA_LOCAL_PATH)
                            .getString(Configs.OTA_LOCAL_PATH, "");
                    Log.i(TAG, "local path =" + spLocalPath);
                    if (spLocalPath.isEmpty()) {
                        FastSharedPreferences.get(Configs.OTA_LOCAL_PATH)
                                .edit().putString(Configs.OTA_LOCAL_PATH, otaPath).apply();
                    } else  {
                        FastSharedPreferences.get(Configs.OTA_SERVER_PATH)
                                .edit().putString(Configs.OTA_SERVER_PATH, otaPath).apply();
                    }
                    break;
                }
                case BleCMDUtil.OTA_INFO_L: {
                    Log.i(TAG, "ota info type=" + Arrays.toString(tlvData.getData()));
                    postEventMessage(new EventBusMessage(EventBusMessage.MESSAGE_TYPE_BLE,
                            new OtaMessage("otaInfo","", tlvData.getData()[0])));
                    break;
                }
                case BleCMDUtil.OTA_VERSION_L: {
                    if (tlvData.getData().length > 1) {
                        int isLocal = tlvData.getData()[0];
                        String otaVersion = StringUtil.INSTANCE.byteConvertString(
                                Arrays.copyOfRange(tlvData.getData(), 1, tlvData.getLength()));
                        Log.i(TAG, "ota version =" + otaVersion + ",isLocal="+isLocal);
                        if (isLocal == 1) {
                            FastSharedPreferences.get(Configs.OTA_LOCAL_VERSION)
                                    .edit().putString(Configs.OTA_LOCAL_VERSION, otaVersion).apply();
                            postEventMessage(new EventBusMessage(EventBusMessage.MESSAGE_TYPE_BLE,
                                    new OtaMessage("localVersion",otaVersion, (byte) 0x00)));
                        } else if (isLocal == 2) {
                            FastSharedPreferences.get(Configs.OTA_SERVER_VERSION)
                                    .edit().putString(Configs.OTA_SERVER_VERSION, otaVersion).apply();
                            postEventMessage(new EventBusMessage(EventBusMessage.MESSAGE_TYPE_BLE,
                                    new OtaMessage("serverVersion",otaVersion, (byte) 0x00)));
                        }
                    }
                    break;
                }
            }
        }
    }

    private void postEventMessage(EventBusMessage message) {
        if (message == null) {
            return;
        }
        EventBus.getDefault().post(message);
    }

    public boolean isConnected() {
        if (mBleService != null) {
            return mBleService.isConnected();
        }
        Log.e(TAG, "the BleService is null");
        return false;
    }

    public void addBluetoothDataCallBack(String tag, IBluetoothDataCallBack bluetoothDataCallBack) {
        if (mBleService != null) {
            mBleService.addBluetoothDataCallBack(tag, bluetoothDataCallBack);
        }
    }

    public void removeBluetoothDataCallBack(String tag) {
        if (mBleService != null) {
            mBleService.removeBluetoothDataCallBack(tag);
        }
    }
}
