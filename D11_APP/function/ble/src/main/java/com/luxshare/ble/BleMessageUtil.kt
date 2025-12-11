package com.luxshare.ble

import android.util.Log
import com.luxshare.ble.util.HexUtil
import java.nio.ByteBuffer


/**
 * aa002 ble message util2
 *
 * @author chence
 * @version 1.0
 *
 */
object BleMessageUtil {
    private val TAG = this::class.java.simpleName



    /**
     * end text:
     * 0        0xAA
     * 1        总长度
     * 2-3      0x5003          表示文本结束
     */
    private val endText = byteArrayOf(0xAA.toByte(), 0x04.toByte(), 0x50.toByte(), 0x13.toByte())

    /**
     * start text:
     * 0        0xAA
     * 1        总长度
     * 2-3      0x5001          表示文本开始
     * 4        长度
     * 5        0x01
     * 6        TST:            Text’s screen time, 之后的文本期望被显示在屏上的最短时间（单位: 秒）；
     * 7        长度
     * 8        0x02
     * 9        Language:       0表示English，1表示Simplified Chinese，2表示Traditional Chinese，其它语言后续待增
     * 10       长度
     * 11       0x03
     * 12       DIR:            此段文本的direction，0表示Source，1表示Destination
     *
     * @param direction: 此段文本的direction，0表示Source，1表示Destination；
     * @param language  0表示English，1表示Simplified Chinese，2表示Traditional Chinese，3 表示Japanese, 其它语言后续待增；
     */
    private fun startText(direction: Int, language: Int): ByteArray {
        if (direction != 0 && direction != 1) {
            throw IllegalArgumentException("翻译dir参数错误，只能传0或1")
        }

        val dirByte = if (direction == 0) 0x00.toByte() else 0x01.toByte()
        val languageByte = when {
            language < 0 -> 0
            language > 3 -> 3
            else -> language.toByte()
        }

        return byteArrayOf(
            0xAA.toByte(), 0x0D.toByte(), 0x50.toByte(), 0x11.toByte(),
            0x02.toByte(), 0x01.toByte(), 0x03.toByte(),
            0x02.toByte(), 0x02.toByte(), languageByte,
            0x02.toByte(), 0x03.toByte(), dirByte
        )
    }


    /**
     * content:
     * 0        AA
     * 1        总长度
     * 2-3      0x5002          表示文本内容
     * 4        CS              高四位：Index，0~15, 数据包序号
     *                          低四位：Coding scheme；0表示UTF-8编码，1表示GBK编码，2表示GB2312编码，3表示Big5编码，4表示Unicode编码
     *
     * @param byteArray     表示文本内容
     * @param codeScheme    0表示UTF-8编码，1表示GBK编码，2表示GB2312编码，3表示Big5编码，4表示Unicode编码
     * @param direction: 此段文本的direction，0表示Source，1表示Destination；
     * @param language  0表示English，1表示Simplified Chinese，2表示Traditional Chinese，3 表示Japanese, 其它语言后续待增；
     */
    fun sendText(
        byteArray: ByteArray,
        codeScheme: Int,
        direction: Int,
        language: Int,
    ): List<ByteArray> {
        val textList = mutableListOf<ByteArray>()
        //如果内容过长，则报错退出
        if (byteArray.size > 16 * 200) {
            Log.e(TAG, "sendText: byteArray(${byteArray.size}) is too long")
            return textList
        }
        textList.add(startText(direction, language))

        var index = 0

        //将内容分割成200字节以内的块
        while (index * 200 < byteArray.size) {
            val data = if (index * 200 + 200 > byteArray.size) {
                byteArray.slice(index * 200 until byteArray.size).toByteArray()
            } else {
                byteArray.slice(index * 200 until index * 200 + 200).toByteArray()
            }

            val len = data.size + 5

            val content = ByteArray(len)

            content[0] = 0xAA.toByte()
            content[1] = len.toByte()
            content[2] = 0x50.toByte()
            content[3] = 0x12.toByte()
            content[4] = (index.shl(4) + codeScheme.and(0x0F)).toByte()

            System.arraycopy(data, 0, content, 5, data.size)

            textList.add(content)
            index++
        }

        textList.add(endText)
        return textList
    }

    /**
     * prompt picture:
     * 0        0xAA
     * 1        总长度
     * 2-3      0x6001          表示提示开始
     * 4        长度
     * 5        0x01
     * 6-13     Area:           prompt显示区域，8个字节，分别是，start_x, start_y, width, height     暂不生效
     * 14       长度
     * 15       0x02
     * 16       Image ID:       图片ID：1表示start，2表示front，4表示left，7表示right
     * 17       长度
     * 18       0x03
     * 19       Image Align:    图片对齐格式      暂不生效
     * 20       长度
     * 21       0x04
     * 22       Text Font:      文本字体         暂不生效
     * 23       长度
     * 24       0x05
     * 25       Text Align:     文本对齐格式      暂不生效
     */
    fun sendPromptPicture(area: ByteArray, pictureId: Int): ByteArray {
        val startPrompt = byteArrayOf(
            0xAA.toByte(),
            0x1A.toByte(),
            0x60.toByte(),
            0x01.toByte(),
            0x09.toByte(),
            0x01.toByte(),
            0x01.toByte(),
            0x02.toByte(),
            0x03.toByte(),
            0x04.toByte(),
            0x05.toByte(),
            0x06.toByte(),
            0x07.toByte(),
            0x08.toByte(),
            0x02.toByte(),
            0x02.toByte(),
            0x01.toByte(),
            0x02.toByte(),
            0x03.toByte(),
            0x00.toByte(),
            0x02.toByte(),
            0x04.toByte(),
            0x01.toByte(),
            0x02.toByte(),
            0x05.toByte(),
            0x00.toByte()
        )

        //如果提示范围格式不对，则报错退出
        if (area.size != 8) {
            Log.e(TAG, "sendPrompt: area is not 8 bytes")
            return byteArrayOf()
        }

        //修改提示范围和图片ID
        System.arraycopy(area, 0, startPrompt, 6, area.size)
        startPrompt[16] = pictureId.toByte()

        return startPrompt
    }

    /**
     * prompt text:
     * 0        AA
     * 1        总长度
     * 2-3      0x6002          表示提示内容
     * 4        CS              高四位：Index, 0:  目的地文本,1：导航文字内容,2：剩余时间/里程文本
     *                          低四位：Coding scheme；0表示UTF-8编码，1表示GBK编码，2表示GB2312编码，3表示Big5编码，4表示Unicode编码
     */
    fun sendPromptText(byteArray: ByteArray, index: Int, codeScheme: Int): ByteArray {
        //如果内容过长，则报错退出
        if (byteArray.size > 200) {
            Log.e(TAG, "sendPrompt: byteArray(${byteArray.size}) is too long")
            return byteArrayOf()
        }

        val len = byteArray.size + 5

        val content = ByteArray(len)

        content[0] = 0xAA.toByte()
        content[1] = len.toByte()
        content[2] = 0x60.toByte()
        content[3] = 0x02.toByte()
        content[4] = (index.shl(4) + codeScheme.and(0x0F)).toByte()

        System.arraycopy(byteArray, 0, content, 5, byteArray.size)

        return content
    }

    /**
     * sync time:
     * 0        AA
     * 1        总长度
     * 2-3      0x7001          表示同步时间
     * 4        长度
     * 5        0x01
     * 6-9      time            表示秒时间戳，4个字节
     */
    fun syncTime(time: Long): ByteArray {

        val timeByteArray =
            ByteBuffer.allocate(Long.SIZE_BITS / Byte.SIZE_BITS).putLong(time).array()

        val syncTime = byteArrayOf(
            0xAA.toByte(), 0x0A.toByte(), 0x70.toByte(), 0x01.toByte(),
            0x05.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte()
        )

        System.arraycopy(timeByteArray, 4, syncTime, 6, 4)
        Log.d(TAG, "syncTime: ${HexUtil.formatHexString(syncTime)}")

        return syncTime
    }

    /**
     * send button:
     * 0        AA
     * 1        总长度
     * 2-3      0x8001          表示按键事件
     * 4        长度
     * 5        0x01
     * 6        button          0x00: KEY_LEFT，0x01: KEY_RIGHT，0x02: KEY_ENTER，0x03: KEY_BACK，0x04: KEY_HOME
     */
    fun sendButton(data: Byte): ByteArray {
        val button = byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0x80.toByte(), 0x01.toByte(),
            0x02.toByte(), 0x01.toByte(), data
        )

        return button
    }

    /**
     * start service:
     * 0        AA
     * 1        总长度
     * 2-3      0x9001          表示启动服务
     * 4        长度
     * 5        0x01
     * 6        service id      0x00：逐句翻译，0x10: 逐字翻译实时，0x01：导航，0x02：实时字幕，0x03：AI助手，0x0F：OTA升级
     */
    fun startService(data: Byte): ByteArray {
        val service = byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0x90.toByte(), 0x01.toByte(),
            0x02.toByte(), 0x01.toByte(), data
        )
        Log.d(TAG, "startService: ${HexUtil.formatHexString(service, true)}")

        return service
    }

    /**
     * end service:
     * 0        AA
     * 1        总长度
     * 2-3      0x9002          表示退出服务
     * 4        长度
     * 5        0x01
     * 6        service id      0x00：逐句翻译，0x10: 逐字翻译实时，0x01：导航，0x02：实时字幕，0x03：AI助手，0x0F：OTA升级
     */
    fun endService(data: Byte): ByteArray {
        val service = byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0x90.toByte(), 0x02.toByte(),
            0x02.toByte(), 0x01.toByte(), data
        )
        Log.d(TAG, "endService: ${HexUtil.formatHexString(service, true)}")

        return service
    }


    /**
     * changeLighting:
     * 0        AA
     * 1        总长度
     * 2-3      0xA001          表示调节亮度
     * 4        长度
     * 5        0x01
     * 6        value           范围：1-100
     */
    fun changeLighting(data: Byte): ByteArray {
        val lighting = when {
            data < 1 -> 1
            data > 100 -> 100
            else -> data
        }

        val byteArray = byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0xA0.toByte(), 0x01.toByte(),
            0x02.toByte(), 0x01.toByte(), lighting
        )

        return byteArray
    }

    /**
     * changeLightingMode:
     * 0        AA
     * 1        总长度
     * 2-3      0xA002          表示手动、自动亮度模式
     * 4        长度
     * 5        0x01
     * 6        value           0x00：关闭，0x01：开启
     */
    fun changeLightingMode(data: Byte): ByteArray {
        val mode = when {
            data < Byte.MIN_VALUE -> Byte.MIN_VALUE
            data > Byte.MAX_VALUE -> Byte.MAX_VALUE
            else -> data
        }

        val byteArray = byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0xA0.toByte(), 0x02.toByte(),
            0x02.toByte(), 0x01.toByte(), mode
        )

        return byteArray
    }

    /**
     * changeWearDetection:
     * 0        AA
     * 1        总长度
     * 2-3      0xA003          表示佩戴检测开关
     * 4        长度
     * 5        0x01
     * 6        value           0x00：关闭，0x01：开启
     */
    fun changeWearDetection(data: Byte): ByteArray {
        val mode = when {
            data < 0 -> 0
            data > 1 -> 1
            else -> data
        }

        val byteArray = byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0xA0.toByte(), 0x03.toByte(),
            0x02.toByte(), 0x01.toByte(), mode
        )

        return byteArray
    }

    /**
     * changeLanguage:
     * 0        AA
     * 1        总长度
     * 2-3      0xB001          表示切换多语言ui
     * 4        长度
     * 5        0x01
     * 6        value           0表示English，1表示Simplified Chinese
     */
    fun changeLanguage(data: Byte): ByteArray {
        val language = when {
            data < 0 -> 0
            data > 1 -> 1
            else -> data
        }

        val byteArray = byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0xB0.toByte(), 0x01.toByte(),
            0x02.toByte(), 0x01.toByte(), language
        )

        return byteArray
    }


    /**
     * start notification:
     * 0        0xAA
     * 1        总长度
     * 2-3      0xC001          表示通知开始
     * 4        长度
     * 5        0x01
     * 6        Type:           表示消息类型，0x01:短信，0x02:微信，0x03:QQ
     * 7        长度
     * 8        0x02
     * 9-12     time            表示秒时间戳，4个字节
     * 13       长度
     * 14       0x03
     * 15-      联系人:          UTF-8格式，如：张三
     */
    private val startNotification = byteArrayOf(
        0xAA.toByte(), 0x0D.toByte(), 0xC0.toByte(), 0x01.toByte(),
        0x02.toByte(), 0x01.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x02.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x04.toByte(),
        0x01.toByte(), 0x03.toByte()
    )

    /**
     * end notification:
     * 0        0xAA
     * 1        总长度
     * 2-3      0xC003          表示通知结束
     */
    private val endNotification =
        byteArrayOf(0xAA.toByte(), 0x04.toByte(), 0xC0.toByte(), 0x03.toByte())


    /**
     * content:
     * 0        AA
     * 1        总长度
     * 2-3      0xC002          表示文本内容
     * 4        CS              高四位：Index，0~15, 数据包序号
     *                          低四位：Coding scheme；0表示UTF-8编码，1表示GBK编码，2表示GB2312编码，3表示Big5编码，4表示Unicode编码
     */
    fun sendNotification(
        byteArray: ByteArray,
        codeScheme: Int,
        notificationType: Int,
        time: Long,
        name: String,
    ): List<ByteArray> {
        val textList = mutableListOf<ByteArray>()
        //如果内容过长，则报错退出
        if (byteArray.size > 16 * 200) {
            Log.e(TAG, "sendText: byteArray(${byteArray.size}) is too long")
            return textList
        }

        if (name.toByteArray().size > 200) {
            Log.e(TAG, "sendText: name($name) is too long")
            return textList
        }

        val nameLength = name.toByteArray(Charsets.UTF_8).size + 2
        val startLength = nameLength + 13

        val tempStartNotification = ByteArray(startLength)
        System.arraycopy(startNotification, 0, tempStartNotification, 0, startNotification.size)

        tempStartNotification[1] = startLength.toByte()
        tempStartNotification[6] = notificationType.toByte()
        tempStartNotification[13] = nameLength.toByte()

        val timeByteArray =
            ByteBuffer.allocate(Long.SIZE_BITS / Byte.SIZE_BITS).putLong(time).array()
        System.arraycopy(timeByteArray, 4, tempStartNotification, 9, 4)

        val nameByteArray = name.toByteArray(Charsets.UTF_8)
        System.arraycopy(nameByteArray, 0, tempStartNotification, 15, nameByteArray.size)

        textList.add(tempStartNotification)

        var index = 0

        //将内容分割成200字节以内的块
        while (index * 200 < byteArray.size) {
            val data = if (index * 200 + 200 > byteArray.size) {
                byteArray.slice(index * 200 until byteArray.size).toByteArray()
            } else {
                byteArray.slice(index * 200 until index * 200 + 200).toByteArray()
            }

            val len = data.size + 5

            val content = ByteArray(len)

            content[0] = 0xAA.toByte()
            content[1] = len.toByte()
            content[2] = 0xC0.toByte()
            content[3] = 0x02.toByte()
            content[4] = (index.shl(4) + codeScheme.and(0x0F)).toByte()

            System.arraycopy(data, 0, content, 5, data.size)

            textList.add(content)
            index++
        }

        textList.add(endNotification)
        return textList
    }

    /**
     * start chat text:
     * 0        0xAA
     * 1        总长度
     * 2-3      0x5031          表示聊天文本开始
     * 4        长度
     * 5        0x01
     * 6        TST:            Text’s screen time, 之后的文本期望被显示在屏上的最短时间（单位: 秒）；
     * 7        长度
     * 8        0x02
     * 9        Language:       0表示English，1表示Simplified Chinese，2表示Traditional Chinese，3 表示Japanese，其它语言后续待增
     * 10       长度
     * 11       0x03
     * 12       ROLE:           当前文本传输角色：0：表示“我“，1：表示”AI“
     */
    fun startChatText(language: Int, role: Int): ByteArray {
        val languageByte = when {
            language < 0 -> 0
            language > 3 -> 3
            else -> language.toByte()
        }

        val roleByte = when {
            role < 0 -> 0
            role > 1 -> 1
            else -> role.toByte()
        }

        return byteArrayOf(
            0xAA.toByte(), 0x0D.toByte(), 0x50.toByte(), 0x31.toByte(),
            0x02.toByte(), 0x01.toByte(), 0x03.toByte(),
            0x02.toByte(), 0x02.toByte(), languageByte,
            0x02.toByte(), 0x03.toByte(), roleByte
        )
    }


    /**
     * 自行编号，整体发送
     * content:
     * 0        AA
     * 1        总长度
     * 2-3      0x5032          表示文本内容
     * 4        CS              高四位：Index，0~15, 数据包序号
     *                          低四位：Coding scheme；0表示UTF-8编码，1表示GBK编码，2表示GB2312编码，3表示Big5编码，4表示Unicode编码
     */
    fun sendChatText(byteArray: ByteArray, codeScheme: Int): List<ByteArray> {
        val textList = mutableListOf<ByteArray>()
        //如果内容过长，则报错退出
        if (byteArray.size > 16 * 200) {
            Log.e(TAG, "sendText: byteArray(${byteArray.size}) is too long")
            return textList
        }

        var index = 0

        //将内容分割成200字节以内的块
        while (index * 200 < byteArray.size) {
            val data = if (index * 200 + 200 > byteArray.size) {
                byteArray.slice(index * 200 until byteArray.size).toByteArray()
            } else {
                byteArray.slice(index * 200 until index * 200 + 200).toByteArray()
            }

            val len = data.size + 5

            val content = ByteArray(len)

            content[0] = 0xAA.toByte()
            content[1] = len.toByte()
            content[2] = 0x50.toByte()
            content[3] = 0x32.toByte()
            content[4] = (index.shl(4) + codeScheme.and(0x0F)).toByte()

            System.arraycopy(data, 0, content, 5, data.size)

            textList.add(content)
            index++
        }

        return textList
    }

    /**
     * 指定序号，单个发送
     * content:
     * 0        AA
     * 1        总长度
     * 2-3      0x5032          表示文本内容
     * 4        CS              高四位：Index，0~15, 数据包序号
     *                          低四位：Coding scheme；0表示UTF-8编码，1表示GBK编码，2表示GB2312编码，3表示Big5编码，4表示Unicode编码
     */
    fun sendChatText(byteArray: ByteArray, codeScheme: Int, index: Int): ByteArray {
        //如果内容过长，则报错退出
        if (byteArray.size > 200) {
            Log.e(TAG, "sendText: byteArray(${byteArray.size}) is too long")
            return byteArrayOf()
        }

        val len = byteArray.size + 5

        val content = ByteArray(len)

        content[0] = 0xAA.toByte()
        content[1] = len.toByte()
        content[2] = 0x50.toByte()
        content[3] = 0x32.toByte()
        content[4] = (index.shl(4) + codeScheme.and(0x0F)).toByte()

        System.arraycopy(byteArray, 0, content, 5, byteArray.size)

        return content
    }

    fun endChatText(): ByteArray {
        return endChatText
    }

    /**
     * end chat text:
     * 0        0xAA
     * 1        总长度
     * 2-3      0x5033          表示文本结束
     */
    private val endChatText =
        byteArrayOf(0xAA.toByte(), 0x04.toByte(), 0x50.toByte(), 0x33.toByte())

    /**
     * change volume:
     * 0        0xAA
     * 1        总长度
     * 2-3      0xE001          APP 改变眼镜音量存储数据
     * 4        长度
     * 5        0x01
     * 6        VOLUME:         用户通过app调节音量，发送给眼镜
     */
    fun changeVolume(volume: Int): ByteArray {
        val volumeByte = when {
            volume < 0 -> 0
            volume > 15 -> 15
            else -> volume.toByte()
        }

        return byteArrayOf(
            0xAA.toByte(), 0x07.toByte(), 0xE0.toByte(), 0x01.toByte(),
            0x02.toByte(), 0x01.toByte(), volumeByte
        )
    }

    /**
     * request volume:
     * 0        0xAA
     * 1        总长度
     * 2-3      0xE002          查询眼镜音量
     */
    fun requestVolume(): ByteArray {
        return byteArrayOf(0xAA.toByte(), 0x04.toByte(), 0xE0.toByte(), 0x02.toByte())
    }

    /**
     * request firmware version:
     * 0        0xAA
     * 1        总长度
     * 2-3      0xF001         查询固件版本号
     */
    fun requestFirmwareVersion(): ByteArray {
        return byteArrayOf(0xAA.toByte(), 0x04.toByte(), 0xF0.toByte(), 0x01.toByte())
    }
}