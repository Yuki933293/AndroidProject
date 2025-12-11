package com.luxshare.base.utils

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date 2024/1/8
 */
object StringUtil {

    /**
     * string数据反转 去除开头0转int
     */
    fun reverseAndRemoveZeros(str: String): Int {
        val reversed = str.reversed()
        val trimmed = reversed.trimStart('0')
        return if (trimmed.isEmpty()) 0 else trimmed.toInt()
    }

    /**
     * byte转String
     */
//    fun byteConvertString(readBuffer: ByteArray?): String {
//       return readBuffer?.decodeToString()?.trim()?:""
//    }

    fun byteConvertString(byteArray: ByteArray): String {
        // 找到最后一个非控制字符的位置
        var lastNonControlCharIndex = byteArray.size - 1
        while (lastNonControlCharIndex >= 0 && byteArray[lastNonControlCharIndex].toInt() == 0) {
            lastNonControlCharIndex--
        }

        // 截取数组，去除最后的控制字符
        val trimmedByteArray = if (lastNonControlCharIndex < byteArray.size - 1) {
            byteArray.copyOf(lastNonControlCharIndex + 1)
        } else {
            byteArray
        }
        // 转换为String
        return String(trimmedByteArray)
    }

//    fun ByteArray.toTrimmedString(charset: Charset = Charsets.UTF_8): String {
//        // 先去除末尾的控制字符
//        val trimmedArray = this.trimTrailingControlCharacters()
//        // 将处理后的 ByteArray 转换为 String
//        return trimmedArray.toString(charset)
//    }
//
//    private fun ByteArray.trimTrailingControlCharacters(): ByteArray {
//        // 从后往前遍历数组，找到第一个不是控制字符的位置
//        for (i in size - 1 downTo 0) {
//            if (this[i] != 0x00.toByte()) {
//                // 复制有效部分到新的数组并返回
//                return this.copyOfRange(0, i + 1)
//            }
//        }
//        // 如果整个数组都是控制字符，返回一个空的 ByteArray
//        return byteArrayOf()
//    }

    /**
     * int转2位字节
     */
    fun intToBytes(value: Int): ByteArray {
        val bytes = ByteArray(2)
        bytes[0] = value.toByte()
        bytes[1] = (value shr 8).toByte()
        return bytes
    }

    fun formatFileSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> "%.2f GB".format(size.toDouble() / gb)
            size >= mb -> "%.2f MB".format(size.toDouble() / mb)
            size >= kb -> "%.2f KB".format(size.toDouble() / kb)
            else -> "$size B"
        }
    }

}