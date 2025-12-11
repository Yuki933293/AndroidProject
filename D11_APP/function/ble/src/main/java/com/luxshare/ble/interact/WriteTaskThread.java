package com.luxshare.ble.interact;

import android.util.Log;

import com.luxshare.ble.Constants;
import com.luxshare.ble.interfaces.IBluetoothService;
import com.luxshare.ble.interfaces.IWriteListener;
import com.luxshare.ble.util.HexUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 写数据
 */
public class WriteTaskThread extends HandlerThreadEx {
    private static final String TAG = "WriteTaskThread";
    private IBluetoothService bluetoothService;
    private IWriteListener writeListener;
    private DataQueue<byte[]> writeQueue;
    private boolean stop = false;

    private WriteTaskThread(String name) {
        super(name);
    }

    public WriteTaskThread(IBluetoothService bluetoothService,
                           IWriteListener writeListener) {
        this(TAG);
        this.writeQueue = new DataQueue<>();
        this.bluetoothService = bluetoothService;
        this.writeListener = writeListener;
    }

    private Runnable writeTask = new Runnable() {
        @Override
        public void run() {
            if (stop) {
                return;
            }
            if (!writeQueue.isEmpty()) {
                if (stop) {
                    return;
                }
                try {
                    byte[] bytes = writeQueue.remove();
                    if (bytes != null) {
                        Log.i(TAG, "send bytes:[" + HexUtil.formatHexString(bytes, ",")+"]");
                        boolean ret = bluetoothService.write(bytes);
                        if (true) {
                            writeSuccess();
                        } else {
                            Log.e(TAG, "send failed:[" + HexUtil.formatHexString(bytes, ",")+"]");
                            writeFailed();
                            //如果失败则追加发送队列重新发送，直到发送成功；
                            writeQueue.add(bytes);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    writeFailed();
                }
            }
            mHandler.postDelayed(this, Constants.WRITE_PERIOD);
        }
    };

    @Override
    public void run() {
        Log.i(TAG, "run: WriteTaskThread");
        super.run();
    }

    public DataQueue<byte[]> getWriteQueue() {
        return writeQueue;
    }

    private void writeSuccess() {
        sMainHandler.post(() -> {
            writeListener.onWriteSuccess();
        });
    }

    private void writeFailed() {
        sMainHandler.post(() -> {
            writeListener.onWriteFailure();
        });
    }

    public void stopThread() {
        stop = true;
    }

    @Override
    protected void onPrepared() {
        if (writeTask != null) {
            mHandler.removeCallbacks(writeTask);
            mHandler.post(writeTask);
            stop = false;
        }

    }

    /**
     * 数据队列
     *
     * @param <T>
     */
    public static class DataQueue<T> {
        private BlockingQueue<T> queue = new LinkedBlockingQueue<T>();

        /**
         * 将数据加入队尾
         *
         * @param data
         */
        public void add(T data) {
            try {
                queue.put(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 将队头数据移出队列
         *
         * @return
         */
        public T remove() {
            try {
                return queue.take();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 数据是否为空
         *
         * @return
         */
        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }
}
