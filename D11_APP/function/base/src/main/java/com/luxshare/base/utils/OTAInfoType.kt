package com.luxshare.base.utils

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2025/1/15 14:03
 */
enum class OTAInfoType(val type: Byte) {
    NO_NETWORK(0x01), UPGRADE_NOTIFICATION(0x02),
    NO_OTA_VERSION(0x03), UPGRADE_FAIL(0x05), WITH_NETWORK(0x06)
}