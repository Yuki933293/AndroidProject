package com.luxshare.ble.eventbus.message

import com.luxshare.ble.eventbus.IEventBusMessage

class NotifyMessage(var uiType: Int = -1, var eventType: Int = -1, var length: Int = 0, var data: ByteArray = byteArrayOf()):
    IEventBusMessage