package com.luxshare.ble.eventbus.message;

import com.luxshare.ble.eventbus.IEventBusMessage;

/**
 * 设备连接状态
 */
public class ConnectState implements IEventBusMessage {
    public ConnectState(String address, boolean status) {
        this.address = address;
        this.status = status;
    }

    private String address;
    private boolean status;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ConnectState{" +
                "address='" + address + '\'' +
                ", status=" + status +
                '}';
    }
}
