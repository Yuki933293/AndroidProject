package com.luxshare.home.repository

import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.luxshare.home.file.LogFileWriter
import java.io.File

/**
 * @desc 文件存储
 *
 * @author hudebo
 * @date  2024/12/9 11:56
 */
class MeetingFileRepository {
    companion object {
        const val TAG = "MeetingFileRepository"
        const val MEETING_PREFIX = "meeting_summary_"
        const val MEETING_PARENT_NAME = "meeting"
    }

    private val externalFile by lazy {
        Environment.getExternalStorageDirectory().getAbsoluteFile()
    }

    private val mTxtFileWriter: LogFileWriter by lazy {
        LogFileWriter(parentPath)
    }

    private val parentPath by lazy {
        externalFile.absolutePath + File.separator + MEETING_PARENT_NAME + File.separator
    }

    /**
     * 保存会议文件 txt
     */
    @Synchronized
    fun saveMeeting2TxtFile(meetingName: String, message: String) {
        mTxtFileWriter.also {
            val lastFileName = it.getLastFileName()
            if (!TextUtils.equals(lastFileName, meetingName)) {
                Log.d(TAG, "开始创建文件")
                //关闭旧文件IO
                if (it.isOpened()) {
                    it.close()
                }
                //创建新文件
                 it.create(meetingName)
                //打开新文件
                if (!it.open()) {
                    Log.d(TAG, "文件创建失败")
                    return
                }
                it.setLastFileName(meetingName)
            }
            it.appendLog(message)
        }
    }

    fun checkFileExists(meetingName: String): Boolean {
        val file = File(parentPath, meetingName)
        return file.exists()
    }

    fun appendMeetingName(meetingName: String): String {
        return "$MEETING_PREFIX$meetingName.txt"
    }

    fun getMeetingFile(meetingName: String): File {
        return File(parentPath, meetingName)
    }

    fun deleteMeetingFile(meetingName: String): Boolean {
        val file = getMeetingFile(meetingName)
        return file.delete()
    }
}