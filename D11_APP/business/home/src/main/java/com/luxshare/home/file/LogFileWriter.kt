package com.luxshare.home.file

import android.util.Log
import java.io.*

/**
 * @desc 文件操作类
 *
 * @author hudebo
 * @date 2023/12/4
 */
class LogFileWriter(private val logDir: String?) {
    private val TAG = "LogFileUtil"
    
    private var mLastFileName: String? = null

    private var mLogFile: File? = null

    private var outputStream: FileOutputStream? = null

    private var mBufferedWriter: BufferedWriter? = null

    fun isOpened(): Boolean {
        return mBufferedWriter != null
    }

    fun getLastFileName(): String? {
        return mLastFileName
    }

    fun setLastFileName(fileName: String): String {
        return fileName.also { mLastFileName = it }
    }

    fun getFile(): File? {
        return mLogFile
    }

    fun create(newFileName: String): Boolean {
        var isCreateFile = false
        Log.i(TAG, "logDir=$logDir;newFileName=$newFileName")
        mLogFile = File(logDir, newFileName)
        mLogFile?.let {
            if (!it.exists()) {
                Log.i(TAG, "${it.absolutePath} 文件不存在")
                try {
                    it.parentFile?.let { parentfile->
                        if (!parentfile.exists()){
                            Log.i(TAG, "parentFile=${parentfile}")
                            parentfile.mkdirs()
                        }
                    }
                    it.createNewFile()
                    Log.i(TAG, "createNewFile")
                    isCreateFile = true
                    Log.i(TAG, "$logDir$newFileName 文件创建成功")
                } catch (e: IOException) {
                    e.printStackTrace()
                    mLastFileName = null
                    mLogFile = null
                    Log.i(TAG, "$logDir$newFileName 文件创建失败")
                }
            }
        }
//        Log.i(TAG, "$newFileName 创建文件成功?$isCreateFile")
        return isCreateFile
    }

    fun open(): Boolean {
        return if (mLogFile == null) {
            return false
        } else {
            try {
                mBufferedWriter = BufferedWriter(FileWriter(mLogFile, true))
                outputStream =  FileOutputStream(mLogFile, true)
                Log.i(TAG, "${mLogFile?.absolutePath} 文件打开成功")
                true
            } catch (e: Exception) {
                e.printStackTrace()
                Log.i(TAG, "${mLogFile?.absolutePath} 文件打开失败")
                mLastFileName = null
                mLogFile = null
                outputStream = null
                false
            }
        }
    }

    fun close(): Boolean {
        return  try {
            mBufferedWriter?.close()
            outputStream?.close()
            Log.i(TAG, "LogFileWriter:close 文件关闭成功")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            Log.i(TAG, "LogFileWriter:close 文件关闭失败")
            false
        } finally {
            mBufferedWriter = null
            mLastFileName = null
            outputStream = null
            mLogFile = null
        }
    }


    fun appendByteLog(bytes: ByteArray): Boolean {
        return try {
            // 可以加个缓存大小；比如 200M 不写入
            outputStream?.write(bytes)
            true
        } catch (e: IOException) {
            Log.e(TAG, "内容写入失败",e)
            false
        }
    }

    fun appendLog(message: String?): Boolean {
        return try {
            // 可以加个缓存大小；比如 200M 不写入
            mBufferedWriter?.write(message)
//            mBufferedWriter?.newLine()
            mBufferedWriter?.flush()
            true
        } catch (e: IOException) {
            Log.e(TAG, "内容写入失败",e)
            false
        }
    }
}