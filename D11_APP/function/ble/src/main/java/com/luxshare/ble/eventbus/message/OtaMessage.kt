package com.luxshare.ble.eventbus.message

import com.luxshare.ble.eventbus.IEventBusMessage

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2025/1/15 10:47
 */
data class OtaMessage(val tag: String, val version: String ="", val infoType: Byte): IEventBusMessage