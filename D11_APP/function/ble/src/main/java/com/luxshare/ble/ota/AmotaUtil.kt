package com.luxshare.ble.ota

import android.util.Log
import com.luxshare.ble.util.HexUtil
import com.luxshare.ble.BleControlUtil.amotaWrite
import com.luxshare.ble.ota.CrcCalculator.calcCrc32
import java.io.FileInputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * aa002 ble am ota util
 *
 * @author chence
 * @version 1.0
 *
 */
class AmotaUtil {
    interface AmotaCallback {
        fun progressUpdate(progress: Int)
        fun error(errorMessage: String)
    }

    enum class eAmotaStatus {
        AMOTA_STATUS_SUCCESS, AMOTA_STATUS_CRC_ERROR, AMOTA_STATUS_INVALID_HEADER_INFO, AMOTA_STATUS_INVALID_PKT_LENGTH, AMOTA_STATUS_INSUFFICIENT_BUFFER, AMOTA_STATUS_UNKNOWN_ERROR, AMOTA_STATUS_MAX
    }

    /* amota commands */
    enum class eAmotaCommand {
        AMOTA_CMD_UNKNOWN, AMOTA_CMD_FW_HEADER, AMOTA_CMD_FW_DATA, AMOTA_CMD_FW_VERIFY, AMOTA_CMD_FW_RESET, AMOTA_CMD_MAX
    }

    private val TAG = this::class.java.simpleName
    private var dataWriteSemaphore: Semaphore? = null
    private var cmdResponseSemaphore: Semaphore? = null
    private val AMOTA_PACKET_SIZE = 512 + 16
    private val AMOTA_FW_PACKET_SIZE = 512
    private val MAXIMUM_APP_PAYLOAD = 20
    private val AMOTA_LENGTH_SIZE_IN_PKT = 2
    private val AMOTA_CMD_SIZE_IN_PKT = 1
    private val AMOTA_CRC_SIZE_IN_PKT = 4
    private val AMOTA_HEADER_SIZE_IN_PKT = AMOTA_LENGTH_SIZE_IN_PKT + AMOTA_CMD_SIZE_IN_PKT
    private var mSelectedFile: String? = null
    private var mStopOta = false
    private var mFsInput: FileInputStream? = null
    private var mFileOffset = 0
    private var mFileSize = 0
    private var mAmotaCallback: AmotaCallback? = null

    private fun formatHex2String(data: ByteArray): String {
        val stringBuilder = StringBuilder(data.size)
        for (byteChar in data) stringBuilder.append(String.format("%02X ", byteChar))
        return stringBuilder.toString()
    }

    private fun waitGATTWriteComplete(timeoutMs: Long): Boolean {
        var ret = false
        try {
            ret = dataWriteSemaphore?.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS) == true
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return ret
    }

    fun setGATTWriteComplete() {
        dataWriteSemaphore?.release()
    }

    private fun waitCmdResponse(timeoutMs: Long): Boolean {
        var ret = false
        try {
            ret = cmdResponseSemaphore?.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS) == true
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return ret
    }

    private fun cmdResponseArrived() {
        cmdResponseSemaphore?.release()
    }

    private fun amOtaCmd2Byte(cmd: eAmotaCommand): Byte {
        when (cmd) {
            eAmotaCommand.AMOTA_CMD_UNKNOWN -> return 0
            eAmotaCommand.AMOTA_CMD_FW_HEADER -> return 1
            eAmotaCommand.AMOTA_CMD_FW_DATA -> return 2
            eAmotaCommand.AMOTA_CMD_FW_VERIFY -> return 3
            eAmotaCommand.AMOTA_CMD_FW_RESET -> return 4
            else -> return 0
        }
    }

    private fun amOtaByte2Cmd(cmd: Int): eAmotaCommand {
        when (cmd and 0xff) {
            1 -> return eAmotaCommand.AMOTA_CMD_FW_HEADER
            2 -> return eAmotaCommand.AMOTA_CMD_FW_DATA
            3 -> return eAmotaCommand.AMOTA_CMD_FW_VERIFY
            4 -> return eAmotaCommand.AMOTA_CMD_FW_RESET
            else -> return eAmotaCommand.AMOTA_CMD_UNKNOWN
        }
    }

    private fun sendOneFrame(data: ByteArray): Boolean {
        if (mStopOta) {
            Log.i(TAG, "OTA stopped due to application control")
        }
        if (!amotaWrite(data)) {
            Log.e(TAG, "Failed to write characteristic")
            return false
        }

        // wait for ACTION_GATT_WRITE_RESULT
        return waitGATTWriteComplete(3000)
    }

    private fun sendPacket(data: ByteArray, len: Int): Boolean {
        var idx = 0
        while (idx < len) {
            var frameLen: Int = if (len - idx > MAXIMUM_APP_PAYLOAD) {
                MAXIMUM_APP_PAYLOAD
            } else {
                len - idx
            }
            val frame = ByteArray(frameLen)
            System.arraycopy(data, idx, frame, 0, frameLen)
            try {
                if (!sendOneFrame(frame)) {
                    return false
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            idx += frameLen
        }
        return true
    }

    private fun sendOtaCmd(cmd: eAmotaCommand, data: ByteArray?, len: Int): Boolean {
        val cmdData = amOtaCmd2Byte(cmd)
        var checksum = 0
        val packetLength = AMOTA_HEADER_SIZE_IN_PKT + len + AMOTA_CRC_SIZE_IN_PKT
        val packet = ByteArray(packetLength)

        // fill data + checksum length
        packet[0] = (len + AMOTA_CRC_SIZE_IN_PKT).toByte()
        packet[1] = (len + AMOTA_CRC_SIZE_IN_PKT shr 8).toByte()
        packet[2] = cmdData
        if (len != 0) {
            // calculate CRC
            checksum = calcCrc32(len, data!!)
            // copy data into packet
            System.arraycopy(data, 0, packet, AMOTA_HEADER_SIZE_IN_PKT, len)
        }

        // append crc into packet
        // crc is always 0 if there is no data only command
        packet[AMOTA_HEADER_SIZE_IN_PKT + len] = checksum.toByte()
        packet[AMOTA_HEADER_SIZE_IN_PKT + len + 1] = (checksum shr 8).toByte()
        packet[AMOTA_HEADER_SIZE_IN_PKT + len + 2] = (checksum shr 16).toByte()
        packet[AMOTA_HEADER_SIZE_IN_PKT + len + 3] = (checksum shr 24).toByte()
        return if (sendPacket(packet, packetLength)) true else {
            Log.e(TAG, "sendPacket failed")
            false
        }
    }

    private fun sendFwHeader(): Boolean {
        val fwHeaderRead = ByteArray(48)
        val ret: Int = mFsInput?.read(fwHeaderRead) ?: 0
        if (ret < 48) {
            Log.w(TAG, "invalid packed firmware length")
            return false
        }
        mFileSize = (fwHeaderRead[11].toInt() and 0xFF shl 24) + (fwHeaderRead[10].toInt() and 0xFF shl 16) +
                (fwHeaderRead[9].toInt() and 0xFF shl 8) + (fwHeaderRead[8].toInt() and 0xFF)
        Log.i(TAG, "mFileSize = $mFileSize")
        Log.i(TAG, "send fw header " + formatHex2String(fwHeaderRead))
        return if (sendOtaCmd(eAmotaCommand.AMOTA_CMD_FW_HEADER, fwHeaderRead, fwHeaderRead.size)) {
            //擦除时间略久，设置超时为10秒
            waitCmdResponse(10000)
        } else false
    }

    private fun sentFwDataPacket(): Int {
        val ret: Int
        var len = AMOTA_FW_PACKET_SIZE
        val fwData = ByteArray(len)
        ret = mFsInput?.read(fwData) ?: 0
        if (ret <= 0) {
            Log.w(TAG, "no data read from mFsInput")
            return -1
        }
        if (ret < AMOTA_FW_PACKET_SIZE) {
            len = ret
            Log.i(TAG, "send fw data len = $len")
        }
        return if (!sendOtaCmd(eAmotaCommand.AMOTA_CMD_FW_DATA, fwData, len)) {
            -1
        } else ret
    }

    private fun sendFwData(): Boolean {
        val fwDataSize = mFileSize
        var ret = -1
        var offset = mFileOffset
        Log.d(TAG, "file size = $mFileSize")
        while (offset < fwDataSize) {
            try {
                ret = sentFwDataPacket()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (ret < 0) {
                Log.e(TAG, "sentFwDataPacket failed")
                return false
            }
            if (!waitCmdResponse(3000)) {
                Log.e(TAG, "waitCmdResponse timeout")
                return false
            }
            offset += ret
            mAmotaCallback?.progressUpdate(offset * 100 / fwDataSize)
        }
        Log.i(TAG, "send firmware data complete")
        return true
    }

    private fun sendVerifyCmd(): Boolean {
        Log.i(TAG, "send fw verify cmd")
        return if (sendOtaCmd(eAmotaCommand.AMOTA_CMD_FW_VERIFY, null, 0)) {
            waitCmdResponse(5000)
        } else false
    }

    private fun sendResetCmd(): Boolean {
        Log.i(TAG, "send fw reset cmd")
        return if (sendOtaCmd(eAmotaCommand.AMOTA_CMD_FW_RESET, null, 0)) {
            waitCmdResponse(3000)
        } else false
    }

    private fun startOtaUpdate() {
        try {
            mFsInput = FileInputStream(mSelectedFile)
            mFileSize = mFsInput?.available() ?: 0
            if (mFileSize == 0) {
                mFsInput?.close()
                Log.w(TAG, "open file error, file path = $mSelectedFile file size = $mFileSize")
                mAmotaCallback?.error("open file error, file path = $mSelectedFile file size = $mFileSize")
                return
            }
            if (!sendFwHeader()) {
                Log.e(TAG, "send FW header failed")
                mFsInput?.close()
                mAmotaCallback?.error("send FW header failed")
                return
            }

            // start to send fw data
            setFileOffset()
            if (!sendFwData()) {
                Log.e(TAG, "send FW Data failed")
                mFsInput?.close()
                mAmotaCallback?.error("send FW Data failed")
                return
            }
            if (!sendVerifyCmd()) {
                Log.e(TAG, "send FW verify cmd failed")
                mFsInput?.close()
                mAmotaCallback?.error("send FW verify cmd failed")
                return
            }

            // need ACK for reset command?
            sendResetCmd()
            mFsInput?.close()
        } catch (e: Exception) {
            e.printStackTrace()
            mAmotaCallback?.error(e.toString())
        }
        Log.i(TAG, "exit startOtaUpdate")
    }

    private fun setFileOffset() {
        if (mFileOffset > 0) {
            Log.i(TAG, "set file offset $mFileOffset")
            mFsInput?.skip(mFileOffset.toLong())
        }
    }

    fun otaCmdResponse(response: ByteArray) {
        val cmd = amOtaByte2Cmd(response[2].toInt() and 0xff)
        if (cmd == eAmotaCommand.AMOTA_CMD_UNKNOWN) {
            Log.e(TAG, "got unknown response" + formatHex2String(response))
            return
        }

        // TODO : handle CRC error and some more here
        if (response[3].toInt() and 0xff != 0) {
            Log.e(TAG, "error occurred, response = " + formatHex2String(response))
            return
        }
        when (cmd) {
            eAmotaCommand.AMOTA_CMD_FW_HEADER -> {
                mFileOffset = (response[4].toInt() and 0xFF) + (response[5].toInt() and 0xFF shl 8) +
                        (response[6].toInt() and 0xFF shl 16) + (response[7].toInt() and 0xFF shl 24)
                Log.i(TAG, "get AMOTA_CMD_FW_HEADER response, mFileOffset = $mFileOffset")
                cmdResponseArrived()
            }

            eAmotaCommand.AMOTA_CMD_FW_DATA -> {
                Log.i(TAG, "get AMOTA_CMD_FW_DATA response, data = ${HexUtil.formatHexString(response)}")
                cmdResponseArrived()
            }

            eAmotaCommand.AMOTA_CMD_FW_VERIFY -> {
                Log.i(TAG, "get AMOTA_CMD_FW_VERIFY response")
                cmdResponseArrived()
            }

            eAmotaCommand.AMOTA_CMD_FW_RESET -> {
                Log.i(TAG, "get AMOTA_CMD_FW_RESET response")
                cmdResponseArrived()
            }

            else -> Log.i(TAG, "get response from unknown command")
        }
    }

    private val updateRunnable = Runnable { startOtaUpdate() }

    fun amOtaStart(filePath: String?, amotaCallback: AmotaCallback?): eAmotaStatus {
        mSelectedFile = filePath
        mStopOta = false
        mAmotaCallback = amotaCallback
        dataWriteSemaphore = Semaphore(0)
        cmdResponseSemaphore = Semaphore(0)
        mFileOffset = 0
        val amOtaStartThread = Thread(updateRunnable)
        amOtaStartThread.start()
        return eAmotaStatus.AMOTA_STATUS_SUCCESS
    }

    fun amOtaStop() {
        mStopOta = true
    }
}