package com.luxshare.ble.eventbus.message;

import com.luxshare.ble.eventbus.IEventBusMessage;

public class ItemViewMessage implements IEventBusMessage {
    private String action;
    /**
     * 当前执行指令昵称
     */
    private String currentCmdNicky;
    /**
     * 用以保存数据值
     */
    private byte[] data = new byte[0];
    /**
     * 返回结果状态
     */
    private boolean state;

    public ItemViewMessage(String action) {
        this.action = action;
    }


    public void setCurrentCmdNicky(String currentCmdNicky) {
        this.currentCmdNicky = currentCmdNicky;
    }

    public void setState(boolean status) {
        this.state = status;
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

    public boolean getState() {
        return state;
    }
}
