package com.luxshare.ble.eventbus

import com.luxshare.ble.eventbus.message.BleMessage
import com.luxshare.ble.eventbus.message.LanguageMessage
import com.luxshare.ble.eventbus.message.NotifyMessage

/**
 * Author: ChenCe
 * Created: 2024/1/22
 * Description: eventbus消息类，所有eventbus消息都集成在该消息中
 */
class EventBusMessage (var tag: String, var message: IEventBusMessage) {

    companion object {
        const val MESSAGE_TYPE_BLE = "message_type_ble"
        const val MESSAGE_TYPE_NOTIFY = "message_type_notify"
        const val MESSAGE_TYPE_ = "message_type_notify"
        const val MESSAGE_TYPE_LANGUAGE = "message_type_language"

        fun getMessageType(tag: String): Class<out IEventBusMessage> {
            return when (tag) {
                MESSAGE_TYPE_BLE -> BleMessage::class.java
                MESSAGE_TYPE_NOTIFY -> NotifyMessage::class.java
                MESSAGE_TYPE_LANGUAGE -> LanguageMessage::class.java
                else -> {
                    IEventBusMessage::class.java
                }
            }
        }
    }
}