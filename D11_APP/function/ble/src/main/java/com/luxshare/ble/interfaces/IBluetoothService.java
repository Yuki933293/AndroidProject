package com.luxshare.ble.interfaces;

/**
 * 蓝牙服务接口
 */
public interface IBluetoothService {
    public boolean write(byte[] value);

    public byte[] read();
}
