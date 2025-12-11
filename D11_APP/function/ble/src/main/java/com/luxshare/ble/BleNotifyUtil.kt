package com.luxshare.ble

import android.util.Log
import com.luxshare.ble.eventbus.EventBusMessage
import com.luxshare.ble.eventbus.message.NotifyMessage
import com.luxshare.ble.util.HexUtil
import org.greenrobot.eventbus.EventBus

/**
 * @author chence
 * @desc ble notify 工具类
 * @date 2024年1月20日10:54:27
 */
object BleNotifyUtil {
    private val TAG = this::class.java.simpleName
    const val UI_TYPE_DESKTOP = 0x00
    const val UI_TYPE_MENU = 0x01
    const val UI_TYPE_HEALTH = 0x02
    const val UI_TYPE_WEATHER = 0x03
    const val UI_TYPE_AIROBOT = 0x04
    const val UI_TYPE_NAVIGATION = 0x05
    const val UI_TYPE_MUSIC = 0x06
    const val UI_TYPE_TIMETEXT = 0x07
    const val UI_TYPE_TRANSLATE = 0x08
    const val UI_TYPE_SCHEDULE = 0x09
    const val UI_TPYE_SETTING = 0x0A
    const val UI_TPYE_BTCONNECT = 0x0B
    const val UI_TPYE_OTA = 0x0C
    const val UI_TPYE_TELEPHONE = 0x0D


    const val GLASS_EVENT_TOUCH_CLICK_START_SPEAK = 0x01
    const val GLASS_EVENT_TOUCH_CLICK_END_SPEAK = 0x02
    const val GLASS_EVENT_TOUCH_CLICK_STOP_RESPONSE = 0x03

    const val GLASS_EVENT_TOUCH_FWD = 0x07
    const val GLASS_EVENT_TOUCH_BACKWD = 0x08
    const val GLASS_EVENT_TOUCH_CLICK = 0x09
    const val GLASS_EVENT_A2DP_VOLUME = 0x0C
    const val GLASS_EVENT_FW_VERSION_UPDATED = 0x34

    const val GLASS_EVENT_TRANSLATE_STATE = 0x56

    /**
     * 解析notify收到的字节数组
     * @param byteArray notify收到的字节数组
     *
     * */
    fun parseData(byteArray: ByteArray) {
        Log.d(TAG, "parseData: [${HexUtil.formatHexString(byteArray, ",")}]")

        val notifyMessage = NotifyMessage()

        if (byteArray.size < 3 || byteArray[0] != 0xBB.toByte()) {
            //Log.e(TAG, "parseData error: size is too short or heart is not 0xBB")
            return
        }

        notifyMessage.uiType = byteArray[1].toInt()
        notifyMessage.eventType = byteArray[2].toInt()

        if (byteArray.size > 5) {
            notifyMessage.length = byteArray[3].toInt().shl(8) + byteArray[4].toInt()

            if (notifyMessage.length + 5 == byteArray.size) {
                notifyMessage.data = byteArray.copyOfRange(5, byteArray.size)
            }
        }

        onNotifyChanged(notifyMessage)
    }

    /**
     * 根据notify的消息，执行相应的操作
     * 需要其他activity处理的消息，通过eventbus抛出
     * @param notifyMessage notify收到的消息解析后的对象
     *
     * */
    private fun onNotifyChanged(notifyMessage: NotifyMessage) {
        when (notifyMessage.uiType) {
            UI_TYPE_AIROBOT -> {
                when (notifyMessage.eventType) {
                    GLASS_EVENT_TOUCH_CLICK -> {
                        Log.d(TAG, "onNotifyChanged: UI_TYPE_AIROBOT, GLASS_EVENT_TOUCH_CLICK")
                        if (notifyMessage.length == 1) {
                            when (notifyMessage.data[0].toInt()) {
                                GLASS_EVENT_TOUCH_CLICK_START_SPEAK -> {
                                }

                                GLASS_EVENT_TOUCH_CLICK_END_SPEAK -> {
                                }

                                GLASS_EVENT_TOUCH_CLICK_STOP_RESPONSE -> {
                                }
                            }
                        }
                    }

                    GLASS_EVENT_TOUCH_FWD, GLASS_EVENT_TOUCH_BACKWD -> {
                        Log.d(
                            TAG,
                            "onNotifyChanged: UI_TYPE_AIROBOT, GLASS_EVENT_TOUCH_FWD or BACKWD"
                        )
                    }
                }
            }

            UI_TYPE_MUSIC -> {
                when (notifyMessage.eventType) {
                    GLASS_EVENT_A2DP_VOLUME -> {
                        Log.d(TAG, "onNotifyChanged: UI_TYPE_MUSIC, GLASS_EVENT_A2DP_VOLUME")
                        EventBus.getDefault().post(
                            EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_NOTIFY,
                                notifyMessage
                            )
                        )
                    }
                }
            }

            UI_TPYE_SETTING -> {
                when (notifyMessage.eventType) {
                    GLASS_EVENT_FW_VERSION_UPDATED -> {
                        Log.d(
                            TAG,
                            "onNotifyChanged: UI_TPYE_SETTING, GLASS_EVENT_FW_VERSION_UPDATED"
                        )
                        EventBus.getDefault().post(
                            EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_NOTIFY,
                                notifyMessage
                            )
                        )
                    }
                }
            }
            /**
             *   增加通话翻译消息接收类型的序列化，需在通话翻译服务中，监听该EventBus消息，并处理
             *   示例如下：
             *
             *     fun onMessageEvent(msg: EventBusMessage) {
             *         when (EventBusMessage.getMessageType(msg.tag)) {
             *             NotifyMessage::class.java -> {
             *                 val message = msg.message as NotifyMessage
             *
             *                 when (message.uiType) {
             *                     BleNotifyUtil.UI_TPYE_TELEPHONE -> {
             *                         when (message.eventType) {
             *                             BleNotifyUtil.GLASS_EVENT_TRANSLATE_STATE -> {
             *                                 if (message.length == 1) {
             *                                     when (message.data[0].toInt()) {
             *                                         0 -> {
             *                                             //todo:关闭对话翻译
             *                                         }
             *
             *                                         1 -> {
             *                                             //todo:开启对话翻译
             *                                         }
             *                                     }
             *                                 }
             *                             }
             *                         }
             *                     }
             *                 }
             *             }
             *         }
             *     }
             *
             * */

            UI_TPYE_TELEPHONE -> {
                when (notifyMessage.eventType) {
                    GLASS_EVENT_TRANSLATE_STATE -> {
                        Log.d(
                            TAG,
                            "onNotifyChanged: UI_TPYE_TELEPHONE, GLASS_EVENT_TRANSLATE_STATE"
                        )
                        EventBus.getDefault().post(
                            EventBusMessage(
                                EventBusMessage.MESSAGE_TYPE_NOTIFY,
                                notifyMessage
                            )
                        )
                    }
                }
            }
        }
    }
}