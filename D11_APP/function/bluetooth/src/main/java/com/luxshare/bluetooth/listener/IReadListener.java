package com.luxshare.bluetooth.listener;

/**
 * The bluetooth read listener.
 *
 * @author ChenCe
 * @version version
 */
public interface IReadListener {

    void onReadSuccess(byte[] readBuffer); // 读成功

    void onReadFailure(); // 读失败
}
