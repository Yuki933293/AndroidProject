package com.luxshare.home.activity

import android.os.Bundle
import android.os.Environment
import com.alibaba.android.arouter.facade.annotation.Route
import com.luxshare.configs.PathConfig
import com.luxshare.home.databinding.ActivityMeetingDetailBinding
import com.luxshare.home.repository.MeetingFileRepository
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2024/12/11 16:23
 */
@Route(path = PathConfig.Path_MeetingDetailActivity)
class MeetingDetailActivity: BaseSpeakerActivity() {
    private lateinit var binding: ActivityMeetingDetailBinding

    private var markwon: Markwon? = null

    private val externalFile by lazy {
        Environment.getExternalStorageDirectory().getAbsoluteFile()
    }

    private val parentPath by lazy {
        externalFile.absolutePath + File.separator +
                MeetingFileRepository.MEETING_PARENT_NAME + File.separator
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeetingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initData()
    }

    private fun initView() {
        binding.titleBar.setClickCallback { finish() }
        markwon = Markwon.create(this)
    }

    private fun initData() {
        val meetingName = intent.extras?.getString("meetingName")?: ""
        binding.titleBar.setTitle(meetingName)
        CoroutineScope(Dispatchers.IO).launch {
            val content = readFileContent(meetingName)
            withContext(Dispatchers.Main) {
                markwon?.setMarkdown(binding.contentTv, content)
            }
        }
    }

    private fun readFileContent(fileName: String): String {
        return try {
            val file = File(parentPath, fileName)
            file.readText()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}