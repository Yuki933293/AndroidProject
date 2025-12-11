package com.luxshare.ble.interact;

import android.util.Log;

import com.luxshare.ble.Constants;
import com.luxshare.ble.eventbus.EventBusMessage;
import com.luxshare.ble.eventbus.message.ResultMessage;
import com.luxshare.ble.interfaces.IBluetoothService;
import com.luxshare.ble.interfaces.IReadListener;
import com.luxshare.ble.util.HexUtil;
import com.luxshare.configs.Configs;
import com.luxshare.configs.ItemCommandManger;

/**
 * 读数据
 */
public class ReadTaskThread extends HandlerThreadEx {
    private static final String TAG = "ReadTaskThread";
    private IBluetoothService bluetoothService;
    private IReadListener readListener;
    /**
     * 用在数据排队解析处理
     */
    private HandlerThreadEx decodeThread = new HandlerThreadEx("data");

    private ReadTaskThread(String name) {
        super(name);
    }

    public ReadTaskThread(IBluetoothService bluetoothService, IReadListener readListener) {
        this(TAG);
        this.bluetoothService = bluetoothService;
        this.readListener = readListener;
    }

    private boolean stop = false;
    private Runnable readTask = new Runnable() {
        @Override
        public void run() {
            if (stop) {
                return;
            }
            byte[] readBuffer = bluetoothService.read();
            if (readBuffer == null) {
                usbReadFailed();
            } else {
                usbReadSuccess(readBuffer);
                decodeThread.mHandler.post(() -> {
                    read2Transform(readBuffer);
                });
            }
            mHandler.postDelayed(this, Constants.READ_PERIOD);
        }
    };

    private void usbReadSuccess(byte[] readBuffer) {
        sMainHandler.post(() -> {
            readListener.onReadSuccess(readBuffer);
        });
    }

    /**
     * 读取并进行解析
     *
     * @param readBuffer
     */
    private void read2Transform(byte[] readBuffer) {
        if (readBuffer == null) {
            Log.e(TAG, "decode: the buffer is null");
            return;
        }
        Log.d(TAG, "read2Transform readBuffer：[" + HexUtil.formatHexString(readBuffer, ",")+"]");
        if (readBuffer.length < 4 || readBuffer.length != readBuffer[1]) {
            Log.e(TAG, "decode: the buffer is error");
            return;
        }
        Log.i(TAG, "read2Transform correct data [" + HexUtil.formatHexString(readBuffer, ",")+"]");
        if (Configs.CMD_RECIEVE_HEAD == readBuffer[0]) {
            byte group = readBuffer[3];
            byte module = readBuffer[4];
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
                }else {
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
        } else {
            Log.i(TAG, "decode: recieve head is different");
        }
    }

    private void postEventMessage(EventBusMessage message) {
        if (message == null) {
            return;
        }
        sMainHandler.post(() -> {
            readListener.onReadEventMessage(message);
        });
    }

    private void usbReadFailed() {
        sMainHandler.post(() -> {
            readListener.onReadFailure();
        });
    }

    public void stopThread() {
        stop = true;
        if (decodeThread.mHandler != null) {
            decodeThread.mHandler.removeCallbacksAndMessages(null);
            sMainHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onPrepared() {
        decodeThread.start();
        mHandler.post(readTask);
    }
}
