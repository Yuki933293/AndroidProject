package com.luxshare.home.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.luxshare.base.share.FileUtil
import com.luxshare.base.share.Share
import com.luxshare.base.share.ShareContentType
import com.luxshare.ble.BleManager
import com.luxshare.configs.PathConfig
import com.luxshare.configs.R
import com.luxshare.home.adapter.MeetingAdapter
import com.luxshare.home.bean.Meeting
import com.luxshare.base.bean.MeetingContent
import com.luxshare.home.bean.Resource
import com.luxshare.home.model.MeetingModel
import com.luxshare.home.repository.MeetingFileRepository
import com.luxshare.home.repository.MeetingRepository
import com.luxshare.home.util.DialogUtil
import com.luxshare.base.utils.BleCMDUtil
import com.luxshare.base.utils.BleCMDUtil.MeetingListEnd
import com.luxshare.base.utils.BleCMDUtil.MeetingList_L
import com.luxshare.base.utils.BleCMDUtil.Meeting_H
import com.luxshare.base.utils.StringUtil
import com.luxshare.resource.databinding.FragmentMeetingBinding
import com.luxshare.resource.databinding.PopMeetingInfoBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import java.util.concurrent.CopyOnWriteArrayList


/**
 * @desc 会议列表
 *
 * @author hudebo
 * @date  2024/12/5 9:48
 */
@Route(path = PathConfig.Path_MeetingFragment)
class MeetingFragment: BaseSpeakerFragment() {
    private lateinit var meetingModel: MeetingModel

    private val meetingList = mutableListOf<Meeting>()

    // 总包的大小
    private var currentBlockTotal = 0
    // 当前包的位置
    private var curentBlockIndex = 0
    // 默认包号
    private val DEFAULT_BLOCK_NUMBER = 0
    // 当前名称
    private var currentMeetingName = byteArrayOf()

    private val REQUEST_SHARE_FILE_CODE = 120

    private val meetingQueue: Queue<ByteArray> = LinkedList()

    // 重发次数
    private var resendCount = 0

    private val contentTotalArray: CopyOnWriteArrayList<MeetingContent> by lazy {
        CopyOnWriteArrayList()
    }

    private val COUNTINTERVAL = 1000L

    private lateinit var binding: FragmentMeetingBinding

    private lateinit var adapter: MeetingAdapter

    private var meetingInfoPop: PopupWindow? = null

    private var popBinding: PopMeetingInfoBinding? = null

    /**
     * 重发机制  60毫秒没有收到数据重发当前包，连续3次之后还是没有收到数据，等待6秒结束发送
     */
    private val mCountDown by lazy {
        object : CountDownTimer(COUNTINTERVAL, COUNTINTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "当前时间=$millisUntilFinished")
            }

            override fun onFinish() {
                // 重新发送上次数据
                Log.d(TAG, "1秒结束")
                tryRequestMeetingContent()
            }
        }
    }

    override fun initChildView(view: View) {
        binding = FragmentMeetingBinding.bind(view)
        titleBar.setTitle(context?.getString(R.string.meeting_minutes_list))
        titleBar.btnOperate.setBackgroundResource(R.drawable.ic_refresh)
        titleBar.btnOperate.setOnClickListener {
            requestMeetingList()
        }
        adapter = MeetingAdapter(object : MeetingAdapter.MeetingClickCallback {
            override fun onClick(meeting: Meeting) {
                gotoMeetingDetail(meeting)
            }

            override fun share(meeting: Meeting) {
                shareMeeting(meeting)
            }

            override fun delete(meeting: Meeting) {
                deleteMeeting(meeting)
            }

            override fun info(meeting: Meeting) {
                showMeetingInfo(meeting)
            }
        })
        val layoutManager = LinearLayoutManager(
            context, RecyclerView.VERTICAL, false
        )
        binding.meetingList.layoutManager = layoutManager
        binding.meetingList.adapter = adapter

        initPopWindow()
    }

    override fun initData() {
        super.initData()
        meetingModel = MeetingModel(MeetingRepository(), MeetingFileRepository())
        requestMeetingList()
        lifecycleScope.launch {
            meetingModel.meetingData.collect {
                Log.i(TAG, "get data success")
                handleInfo(it)
            }
        }
    }

    private fun handleInfo(data: Resource<ByteArray>) {
        when (data) {
            is Resource.Loading -> {
                Log.d(TAG, "start")
            }
            is Resource.Error -> {
                data.exception.message?.let { Log.e(TAG, it) }
                synchronousMeetingFail()
            }
            is Resource.Success -> {
                dealTLVCmdData(data.data)
            }
        }
    }

    // 异常 处理
    private fun dealTLVCmdData(byteArray: ByteArray) {
        val data = BleCMDUtil.parseMeetingTLVData(byteArray)
        Log.i(TAG, "dealTLVCmdData=$data")
        data?.let {
            if (it.moduleId == Meeting_H) {
                resetCount()
                when (it.operationId) {
                    MeetingList_L -> {
                        Log.i(TAG, "获取到会议列表")
                        if (it.data.contentEquals(MeetingListEnd)) {
                            Log.i(TAG, "获取会议列表结束")
                            processMeetings()
                        } else {
                            val meeting = Meeting(meetingModel.appendMeetingName(
                                StringUtil.byteConvertString(it.data)), it.data)
                            meetingList.add(meeting)
                            meetingQueue.add(it.data)
                        }
                    }

                    BleCMDUtil.MeetingWrap_L -> {
                        if (it.data.size > 21) {
                            currentBlockTotal = (it.data[0].toInt()) or
                                    (it.data[1].toInt() and 0xFF shl 8)
                            currentMeetingName = it.data.copyOfRange(2, it.length)
                        }
                        Log.i(
                            TAG, "当前会议总包=${currentBlockTotal}," +
                                    "当前会议名称=${StringUtil.byteConvertString(currentMeetingName)}"
                        )
                        // 总包块获取成功，从index 0包块开始获取包块内的会议内容
                        requestMeetingContent()
                    }

                    BleCMDUtil.MeetingContent_L -> {
                        // 获取到包块内容，如果得到的数据包号!=当前包号且会议名称!=当前会议名称,
                        // 则继续发送当前包号重新获取;每个包块内容的重试次数为3次，如果3次还未获取到则整体结束
                        val meetingContent = BleCMDUtil.parseMeetingContent(it.length, it.data)
                        if (meetingContent == null) {
                            // 重发
                            Log.i(TAG,"当前包数据是空，继续取当前的包数据")
                            requestMeetingContent()
                            return
                        }
                        Log.i(TAG, "返回数据的index=${meetingContent.meetingIndex}," +
                                "content=${StringUtil.byteConvertString(meetingContent.content)}")
                        when(meetingContent.meetingIndex) {
                            // 当前包== 总包数量 表示所有包都取完
                            currentBlockTotal -> {
                                Log.i(TAG,"所有的包会议数据已经取完")
                                // 保存会议数据到文件中
                                saveMeetingContent2File()
                            }
                            // 如果得到的数据包号==当前包号且会议名称==当前会议名称 获取下一条
                            curentBlockIndex -> {
                                Log.d(TAG,"当前包数据完成，取下一个包日志数据，当前包是=$curentBlockIndex," +
                                        "返回会议名称=${StringUtil.byteConvertString(meetingContent.meetingName)}," +
                                        "当前会议名称=$${StringUtil.byteConvertString(currentMeetingName)}")
                                if (currentMeetingName.contentEquals(meetingContent.meetingName)) {
                                    if (!contentTotalArray.contains(meetingContent)) {
                                        contentTotalArray.add(meetingContent)
                                    }
                                    curentBlockIndex++
                                    requestMeetingContent()
                                } else {
                                    // 重发
                                    requestMeetingContent()
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun processMeetings() {
        if (meetingList.isEmpty()) {
            synchronousNoMeeting()
        }else {
            sendMeeting2File()
        }
    }

    private fun sendMeeting2File() {
        releaseMeeting()
        if (meetingQueue.isEmpty()) {
            synchronousMeetingSuccess()
        } else {
            val meeting = meetingQueue.poll()
            meeting?.let {
                val name = meetingModel.appendMeetingName(StringUtil.byteConvertString(meeting))
                val fileExists = meetingModel.meetingFileExists(name)
                Log.i(TAG, "fileExists=$fileExists;name=$name")
                if (fileExists) {
                    sendMeeting2File()
                } else {
                    requestMeetingTotalBlocks(meeting)
                }
            }
        }
    }

    // 当前的所有已经完成，保存会议到文件中， 继续去下一个会议数据
    private fun saveMeetingContent2File() {
        Log.i(TAG, "saveMeetingContent2File")
        // 计算总长度
        val totalSize = contentTotalArray.sumOf { it.content.size }
        // 创建一个新的ByteArray，其大小是所有ByteArray大小的总和
        val combinedByteArray = ByteArray(totalSize)
        // 用于记录当前复制到的位置
        var currentPosition = 0
        contentTotalArray.forEach {
            System.arraycopy(it.content, 0, combinedByteArray, currentPosition, it.content.size)
            currentPosition += it.content.size
        }

        val meeting = contentTotalArray[0]
        val tempMeeting = MeetingContent(meeting.meetingName, 0, combinedByteArray)
        meetingModel.saveMeetingContent2File(tempMeeting)
        sendMeeting2File()
    }

    // 同步会议成功 显示页面，展示数据
    private fun synchronousMeetingSuccess() {
        Log.i(TAG, "synchronousMeetingSuccess")
        val endTime = System.currentTimeMillis()
        Log.i(TAG, "总时间:${endTime - startTime}")
        titleBar.btnOperate.setVisibility(View.VISIBLE)
        binding.meetingList.visibility = View.VISIBLE
        binding.stateLayout.visibility = View.GONE

        adapter.setList(meetingList.sortedByDescending  {
            it.meetingName
        })
    }

    // 同步会议失败 显示页面，展示数据
    private fun synchronousMeetingFail() {
        Log.i(TAG, "synchronousMeetingFail")
        titleBar.btnOperate.setVisibility(View.VISIBLE)
        binding.meetingList.visibility = View.GONE
        binding.stateLayout.visibility = View.VISIBLE
        binding.statusImg.setImageResource(R.mipmap.syncfail_img)
        binding.statusTv.text = context?.getString(R.string.synchronizing_meeting_fail)
    }

    private fun synchronousMeetingLoading() {
        Log.i(TAG, "synchronousMeetingLoading")
        titleBar.btnOperate.setVisibility(View.GONE)
        binding.meetingList.visibility = View.GONE
        binding.stateLayout.visibility = View.VISIBLE
        binding.statusImg.setImageResource(R.mipmap.sync_img)
        binding.statusTv.text = context?.getString(R.string.synchronizing_meeting)
    }

    private fun synchronousNoMeeting() {
        Log.i(TAG, "synchronousNoMeeting")
        titleBar.btnOperate.setVisibility(View.VISIBLE)
        binding.meetingList.visibility = View.GONE
        binding.stateLayout.visibility = View.VISIBLE
        binding.statusImg.visibility = View.GONE
        binding.statusTv.text = context?.getString(R.string.no_meeting_minutes)
    }

    // 释放会议相关对象
    private fun releaseMeeting() {
        Log.i(TAG, "releaseMeeting")
        curentBlockIndex = DEFAULT_BLOCK_NUMBER
        currentBlockTotal = DEFAULT_BLOCK_NUMBER
        contentTotalArray.clear()
    }

    private var startTime = 0L

    /**
     *  请求会议列表
     */
    private fun requestMeetingList(){
        Log.i(TAG, "请求会议列表")
        startTime = System.currentTimeMillis()
        meetingList.clear()
        Log.i(TAG, "开始时间:${startTime}")
        if (BleManager.getInstance().isConnected) {
            synchronousMeetingLoading()
            meetingModel.fetchMeetings(BleCMDUtil.meetingListCMD())
        } else {
            synchronousMeetingFail()
        }
    }

    /**
     *  请求会议总包号
     */
    private fun requestMeetingTotalBlocks(meetingName: ByteArray){
        Log.i(TAG, "请求会议总包号 =${StringUtil.byteConvertString(meetingName)}")
        meetingModel.fetchMeetings(BleCMDUtil.meetingWrapCMD(meetingName))
    }

    /**
     * 请求会议内容
     */
    private fun requestMeetingContent() {
        Log.i(TAG, "当前会议名称->${StringUtil.byteConvertString(currentMeetingName)}" +
                ", 当前会议编号->$curentBlockIndex")
        meetingModel.fetchMeetings(BleCMDUtil.meetingContentCMD(currentMeetingName, curentBlockIndex))
        mCountDown.cancel()
        mCountDown.start()
    }

    /**
     * 重试3次
     */
    private fun tryRequestMeetingContent() {

    }

    private fun resetCount() {
        mCountDown.cancel()
        resendCount = 0
    }

    private fun deleteMeeting(meeting: Meeting) {
        DialogUtil.showDeleteDialog(requireContext()) {
            meetingModel.fetchMeetings(BleCMDUtil.deleteMeetingCMD(meeting.meetingNameByte))
            // 删除文件
            val delete = meetingModel.deleteMeetingFile(meeting.meetingName)
            // 刷新列表
            if (delete) {
                adapter.delete(meeting)
                meetingList.remove(meeting)
                Log.i(TAG, "meetingList size="+meetingList.size)
                if (adapter.itemCount == 0) {
                    synchronousNoMeeting()
                }
            }
        }
    }

    private fun gotoMeetingDetail(meeting: Meeting) {
        val file = meetingModel.getMeetingFile(meeting.meetingName)
        if (fileNotExistTips(file)) return
        ARouter.getInstance().build(PathConfig.Path_MeetingDetailActivity)
            .withString("meetingName", meeting.meetingName).navigation()
    }

    private fun shareMeeting(meeting: Meeting) {
        Log.i(TAG, "shareMeeting")
        val file = meetingModel.getMeetingFile(meeting.meetingName)
        if (fileNotExistTips(file)) return
        Share.Builder(requireActivity())
            .setContentType(ShareContentType.FILE)
            .setShareFileUri(
                FileUtil.getFileUri(requireContext(), ShareContentType.FILE,
                    file))
            .setTitle(meeting.meetingName)
            .setOnActivityResult(REQUEST_SHARE_FILE_CODE)
            .build()
            .shareBySystem()
    }

    private fun showMeetingInfo(meeting: Meeting) {
        val file = meetingModel.getMeetingFile(meeting.meetingName)
        if (fileNotExistTips(file)) return

        val lastModified = file.lastModified() // 获取最后修改时间（毫秒值）
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val time = dateFormat.format(Date(lastModified))

        popBinding?.let {
            it.meetingName.text = meeting.meetingName
            it.filePath.text = file.absolutePath
            it.fileSize.text = StringUtil.formatFileSize(file.length())
            it.saveTime.text = time
        }

        meetingInfoPop?.showAtLocation(binding.root, Gravity.BOTTOM, 0, 0)
    }

    private fun fileNotExistTips(file: File): Boolean {
        if (!file.exists() || !file.isFile) {
            Toast.makeText(
                requireContext(), requireContext()
                    .getString(com.luxshare.resource.R.string.file_not_exist_tip),
                Toast.LENGTH_SHORT
            ).show()
            return true
        }
        return false
    }

    private fun initPopWindow() {
        popBinding = PopMeetingInfoBinding.inflate(layoutInflater)
        meetingInfoPop = PopupWindow(
            popBinding?.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        ).apply {
            isOutsideTouchable = false
            isFocusable = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        popBinding?.closeBtn?.setOnClickListener {
            meetingInfoPop?.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "resultCode=$resultCode,requestCode=$requestCode")
        if (requestCode == REQUEST_SHARE_FILE_CODE){
            Log.i(TAG, "share success")
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public fun onMessageEvent(msg: EventBusMessage) {
//        val message = msg.message
//        if (message is ConnectState) {
//            Log.i(TAG, "device connectStatus :" + message.status)
//            if (!message.status) {
//                if (message.address.equals(deviceAddress, ignoreCase = true)) {
//                    DialogUtil.showReconnectDialog(requireContext()) {
//                        startActivity(Intent(requireContext(), HomeActivity::class.java))
//                        back()
//                    }
//                }
//            }
//        }
//    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        meetingModel.release()
    }

    override fun getChildLayoutId(): Int = R.layout.fragment_meeting
}