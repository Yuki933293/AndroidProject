package com.luxshare.ble.eventbus.message;

import com.luxshare.ble.eventbus.IEventBusMessage;

import java.util.UUID;

public class BleMessage implements IEventBusMessage {
    private String action;
    private UUID characteristicUUID = UUID.randomUUID();
    /**
     * 用以保存数据值
     */
    private byte[] data = new byte[0];
    /**
     * 当前执行指令昵称
     */
    private String currentCmdNicky;
    /**
     * 返回结果状态
     */
    private int status;

    public BleMessage(String action) {
        this.action = action;
    }

    public BleMessage(String action, UUID characteristicUUID, int status) {
        this(action, characteristicUUID, new byte[0], 0);
    }

    public BleMessage(String action, UUID characteristicUUID, byte[] data) {
        this(action, characteristicUUID, data, 0);
    }

    public BleMessage(String action, UUID characteristicUUID, byte[] data, int status) {
        this.action = action;
        this.characteristicUUID = characteristicUUID;
        this.data = data;
        this.status = status;
    }

    public void setCurrentCmdNicky(String currentCmdNicky) {
        this.currentCmdNicky = currentCmdNicky;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCurrentCmdNicky() {
        return currentCmdNicky;
    }

    public int getStatus() {
        return status;
    }
}
