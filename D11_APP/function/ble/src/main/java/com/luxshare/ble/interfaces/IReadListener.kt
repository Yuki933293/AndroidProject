package com.luxshare.ble.interfaces

import com.luxshare.ble.eventbus.EventBusMessage

/**
 * The bluetooth read listener.
 *
 * @author ChenCe
 * @version version
 */
interface IReadListener {
    fun onReadSuccess(readBuffer: ByteArray) // 读成功
    fun onReadFailure() // 读失败
    fun onReadEventMessage(eventBusMessage: EventBusMessage) {
    }
}
