package com.luxshare.bluetooth.listener;

/**
 * The bluetooth write listener.
 *
 * @author ChenCe
 * @version version
 */
public interface IWriteListener {

    void onWriteSuccess(); // 写成功

    void onWriteFailure(); // 写失败
}
