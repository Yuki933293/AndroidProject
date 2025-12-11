package com.luxshare.base.bean

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2024/12/9 10:17
 */
data class BleTLVData(val sof: Byte, val moduleId: Byte, val operationId: Byte,
                      val length: Int, val data: ByteArray)