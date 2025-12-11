package com.luxshare.home.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luxshare.base.bean.MeetingContent
import com.luxshare.home.bean.Resource
import com.luxshare.home.repository.MeetingFileRepository
import com.luxshare.home.repository.MeetingRepository
import com.luxshare.base.utils.StringUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2024/12/5 10:29
 */
class MeetingModel(private val meetingRepository: MeetingRepository,
                   private val fileRepository: MeetingFileRepository) : ViewModel() {
    private val TAG = "MeetingModel"

    val meetingData = MutableStateFlow<Resource<ByteArray>>(Resource.Loading())

    // 需要处理无数据问题 或者异常

    fun fetchMeetings(byteArray: ByteArray) = viewModelScope.launch {
        meetingRepository.fetchMeetingData(byteArray).map {
            it
        }.catch {
            Log.e(TAG, it.message?: "")
            meetingData.value = Resource.Error(it)
        }.collect {
            meetingData.value = Resource.Success(it)
        }
    }

    @Synchronized
    fun saveMeetingContent2File(meetingContent: MeetingContent) {
        val name = StringUtil.byteConvertString(meetingContent.meetingName)
        val content = StringUtil.byteConvertString(meetingContent.content)
        Log.i(TAG, "name=$name, content=$content")
        fileRepository.saveMeeting2TxtFile(fileRepository.appendMeetingName(name), content)
    }

    fun appendMeetingName(meetingName: String): String {
        return fileRepository.appendMeetingName(meetingName)
    }

    fun meetingFileExists(meetingName: String): Boolean {
        return fileRepository.checkFileExists(meetingName)
    }

    fun getMeetingFile(meetingName: String): File {
        return fileRepository.getMeetingFile(meetingName)
    }

    fun deleteMeetingFile(meetingName: String): Boolean {
        return fileRepository.deleteMeetingFile(meetingName)
    }

    fun reset() {
        meetingData.value = Resource.Loading()
    }

    fun pause() {
        meetingRepository.pause()
    }

    fun release() {
        meetingRepository.release()
    }
}