package com.luxshare.ble.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.luxshare.ble.Constants;
import com.luxshare.ble.R;
import com.luxshare.configs.ContextManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BleScanHandler {
    private static final String TAG = "BleScanUtils";
    private static BleScanHandler instance;
    private final Context sContext;
    private Handler sHandler = new Handler(Looper.getMainLooper());
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter sBluetoothAdapter;
    private BluetoothLeScanner mScanner;
    private boolean mScanning;

    private BleScanHandler() {
        sContext = ContextManager.getInstance().getContext();
        bluetoothManager = (BluetoothManager) ContextManager.getInstance().getContext()
                .getSystemService(Context.BLUETOOTH_SERVICE);
        sBluetoothAdapter = bluetoothManager.getAdapter();
        mScanner = sBluetoothAdapter.getBluetoothLeScanner();
        Log.i(TAG, "init: sBluetoothAdapter:" + sBluetoothAdapter);
    }

    public static BleScanHandler getInstance() {
        if (instance == null) {
            instance = new BleScanHandler();
        }
        return instance;
    }

    public void startScan() {
        scanLeDevice(true);
    }

    public void stopScan() {
        scanLeDevice(false);
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return sBluetoothAdapter;
    }

    public boolean isScanning() {
        return mScanning;
    }

    @SuppressLint("MissingPermission")
    public void openBleSwitch() {
        if (!sBluetoothAdapter.isEnabled()) {
            sBluetoothAdapter.enable();
        }
    }

    public boolean isBlueSwitchOpen() {
        return sBluetoothAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(boolean enable) {
        Log.i(TAG, "scanLeDevice: enable:" + enable);
        // 搜索BLE设置之前 需要刷新下BT
        sBluetoothAdapter.startDiscovery();
        sBluetoothAdapter.cancelDiscovery();

        if (!sBluetoothAdapter.isEnabled()) {
            Toast.makeText(sContext, sContext.getString(R.string.bluetooth_switch)
                    , Toast.LENGTH_SHORT).show();
            return;
        }
        if (mScanner == null) {
            mScanner = sBluetoothAdapter.getBluetoothLeScanner();
        }
        if (enable) {
            Log.i(TAG, "mScanning: " + mScanning);
            if (mScanning) {
                return;
            }
            sHandler.removeCallbacksAndMessages(null);
            sHandler.postDelayed(() -> {
                mScanning = false;
                mScanner.stopScan(bleScanCallback);
                scanEndCall();
            }, Constants.BLE_SCAN_PERIOD);
            mScanning = true;
            perScanEndState = false;
            for (BleScanCallback scanCallback : scanCallbacks) {
                scanCallback.onScanResultStart();
            }
            mScanner.startScan(bleScanCallback);
        } else {
            if (!mScanning) {
                return;
            }
            mScanning = false;
            mScanner.stopScan(bleScanCallback);
            scanEndCall();
        }
    }

    @SuppressLint("MissingPermission")
    public void release() {
        if (mScanner != null) {
            mScanner.stopScan(bleScanCallback);
        }
        scanCallbacks.clear();
    }

    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getConnectedDevice() {
        Set<BluetoothDevice> bondedDevices = getBondedDevices();
        Set<BluetoothDevice> connectedDevices = new HashSet<>();
        if (bondedDevices != null) {
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                try {
                    //获取当前连接的蓝牙信息
                    Boolean isConnected = (Boolean) bluetoothDevice.getClass().getMethod("isConnected")
                            .invoke(bluetoothDevice);
                    if (isConnected) {
                        connectedDevices.add(bluetoothDevice);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return connectedDevices;
    }

    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getBondedDevices() {

        return sBluetoothAdapter.getBondedDevices();
    }

    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
//            Log.i(TAG, "onScanResult: ");
            for (BleScanCallback scanCallback : scanCallbacks) {
                scanCallback.onScanResult(callbackType, result);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.i(TAG, "onBatchScanResults: ");
            scanEndCall();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG, "onScanFailed: errorCode:" + errorCode);
            for (BleScanCallback scanCallback : scanCallbacks) {
                scanCallback.onScanFailed(errorCode);
            }
            scanEndCall();
        }
    };

    /**
     * 扫描是否结束
     */
    private boolean perScanEndState = false;

    private void scanEndCall() {
        if (!perScanEndState) {
            perScanEndState = true;
            for (BleScanCallback scanCallback : scanCallbacks) {
                scanCallback.onScanResultEnd();
            }
        }
    }

    public void addBleScanCallback(BleScanCallback callback) {
        if (!scanCallbacks.contains(callback)) {
            scanCallbacks.add(callback);
        }
    }

    public void removeBleScanCallback(BleScanCallback callback) {
        if (scanCallbacks.contains(callback)) {
            scanCallbacks.remove(callback);
        }
    }

    private List<BleScanCallback> scanCallbacks = new ArrayList<>();

    public static interface BleScanCallback {
        /**
         * 扫描开启
         */
        default void onScanResultStart() {
        }

        void onScanResult(int callbackType, ScanResult result);

        default void onScanFailed(int errorCode) {
        }

        /**
         * 扫描结束
         */
        default void onScanResultEnd() {
        }
    }
}
