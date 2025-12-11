package com.luxshare.ble;

/**
 * 外围蓝牙设备链接状态
 */
final public class DeviceState {
    /**
     * 断开
     */
    public final static int STATE_DISCONNECTED = 0;
    /**
     * 链接中
     */
    public final static int STATE_CONNECTING = 1;
    /**
     * 已连接
     */
    public final static int STATE_CONNECTED = 2;
    /**
     * 查找
     */
    public final static int STATE_SERVICES_DISCOVERED = 3;
}
