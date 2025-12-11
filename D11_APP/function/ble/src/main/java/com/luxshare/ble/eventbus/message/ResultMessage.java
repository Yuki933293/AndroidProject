package com.luxshare.ble.eventbus.message;

import com.luxshare.ble.eventbus.IEventBusMessage;

public class ResultMessage implements IEventBusMessage {
    /**
     * 用于表示群组id
     */
    private byte group;
    /**
     * 用于表示模块id
     */
    private byte nicky;
    /**
     * 返回结果状态
     */
    private int result;

    public byte getGroup() {
        return group;
    }

    public void setGroup(byte group) {
        this.group = group;
    }

    public byte getNicky() {
        return nicky;
    }

    public void setNicky(byte nicky) {
        this.nicky = nicky;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
