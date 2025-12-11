package com.luxshare.home.fragment

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.alibaba.android.arouter.facade.annotation.Route
import com.luxshare.base.bean.BleTLVData
import com.luxshare.base.utils.BleCMDUtil
import com.luxshare.base.utils.BleCMDUtil.parseMeetingTLVData
import com.luxshare.base.utils.OTAInfoType
import com.luxshare.base.utils.StringUtil
import com.luxshare.base.utils.VerificationURLUtil
import com.luxshare.ble.BleManager
import com.luxshare.ble.eventbus.EventBusMessage
import com.luxshare.ble.eventbus.message.OtaMessage
import com.luxshare.ble.interfaces.IBluetoothDataCallBack
import com.luxshare.configs.Configs
import com.luxshare.configs.PathConfig
import com.luxshare.fastsp.FastSharedPreferences
import com.luxshare.resource.R
import com.luxshare.resource.databinding.FragmentOatBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2025/1/15 10:11
 */
@Route(path = PathConfig.Path_OtaFragment)
class OtaFragment : BaseSpeakerFragment() {
    private lateinit var binding: FragmentOatBinding
    private var stepHashMap: HashMap<String, Boolean> = HashMap()
    private var upgrading: Boolean = false
    private var editAddress = ""
    private val setp_download = "download"
    private val setp_version = "version"
    private val setp_battery = "battery"
    private val setp_network = "network"
    private val e_mail = "Quanfeng.Wang@luxshare-ict.com"

    private val bluetoothDataCallBack by lazy {
        object : IBluetoothDataCallBack {
            override fun blueDataCallBack(readBuffer: ByteArray) {
                val tlvData = parseMeetingTLVData(readBuffer)
                dealTlvData(tlvData)
            }
        }
    }

    override fun initChildView(view: View) {
        binding = FragmentOatBinding.bind(view)
        initView()
        binding.addressEdit.setOnClickListener {
            if (editDialog?.isShowing == true) {
                return@setOnClickListener
            }
            showEditDialog()
        }
        binding.stateBtn.setOnClickListener {
            Log.i(TAG, "stepHashMap=$stepHashMap")
            if (stepHashMap.containsKey(setp_download)
                && stepHashMap[setp_download] == true
            ) {
                upgrade()
                return@setOnClickListener
            }
            if (stepHashMap[setp_battery] == true
                && stepHashMap[setp_network] == true
                && stepHashMap[setp_version] == false
            ) {
                // 发邮件
                sendEmail()
                return@setOnClickListener
            }
            if (stepHashMap[setp_battery] == true &&
                stepHashMap[setp_network] == true &&
                stepHashMap[setp_version] == true &&
                stepHashMap.containsKey(setp_download)
                && stepHashMap[setp_download] == false
            ) {
                Log.i(TAG, "recheck")
                initView()
                checkUpgrading()
            }
        }

        titleBar.setClickCallback {
            if (upgrading) {
                return@setClickCallback
            }
            onPopBack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.i(TAG, "upgrading=$upgrading")
                    if (upgrading) {
                        return
                    }
                    onPopBack()
                }
            })
    }

    override fun initData() {
        super.initData()
        BleManager.getInstance().addBluetoothDataCallBack(TAG, bluetoothDataCallBack)
        checkUpgrading()
    }

    private fun initView() {
        binding.apply {
            upgrading = false
            stepHashMap.clear()
            binding.upgradeGroup.visibility = View.VISIBLE
            binding.tipGroup.visibility = View.GONE
            otaAttention.visibility = View.INVISIBLE
            if (editAddress.isEmpty()) {
                val otaUrl = getAvailableURL()
                Log.i(TAG, "otaUrl=$otaUrl")
                if (otaUrl?.isNotEmpty() == true) {
                    otaAddress.text = otaUrl
                }
            } else {
                binding.otaAddress.text = editAddress
            }
            checkNetTv.text = getString(R.string.check_network)
            connectLoading.visibility = View.VISIBLE
            connectState.visibility = View.GONE
            checkBatteryTv.text = getString(R.string.check_battery)
            batteryLoading.visibility = View.VISIBLE
            batteryState.visibility = View.GONE
            val localVersionValue = FastSharedPreferences.get(Configs.OTA_LOCAL_VERSION)
                .getString(Configs.OTA_LOCAL_VERSION, "")
            Log.i(TAG, "localVersionValue=$localVersionValue")
            if (localVersionValue?.isNotEmpty() == true) {
                localVersion.text = localVersionValue
            }
            latestVersion.text = "..."
            latestVersion.setTextColor(
                resources.getColor(
                    R.color.ota_text_black_color,
                    requireContext().theme
                )
            )
            progressbar.progress = 0
            stateBtn.text = getString(R.string.checking_upgrade)
        }
    }

    private fun checkUpgrading() {
        Log.i(TAG, "editAddress=$editAddress")
        BleManager.getInstance().add(BleCMDUtil.queryMessageCMD(BleCMDUtil.OTA_BATTERY_L))
        BleManager.getInstance().add(BleCMDUtil.queryMessageCMD(BleCMDUtil.OTA_NETWORK_L))
    }

    private fun checkAddressVersion() {
        stepHashMap.remove(setp_version)
        if (editAddress.isEmpty()) {
            BleManager.getInstance().add(BleCMDUtil.queryVersionCMD(2))
        } else {
            BleManager.getInstance().add(BleCMDUtil.queryOTAAddressVersion(editAddress))
        }
    }

    override fun getBarTitle(): CharSequence {
        return getString(R.string.ota_title)
    }

    private fun dealTlvData(readBuffer: BleTLVData?) {
        readBuffer?.let {
            if (it.operationId == BleCMDUtil.OTA_BATTERY_L) {
                val battery = it.data[0].toInt()
                checkBattery(battery)
            }
            if (it.operationId == BleCMDUtil.OTA_PROGRESS_L) {
                val progress = StringUtil.byteConvertString(it.data).toInt()
                Log.i(TAG, "progress=$progress")
                upgrading = true
                binding.progressbar.progress = progress
                binding.stateBtn.text =
                    String.format(getString(R.string.download_progress), progress)
                if (progress == 100) {
                    requireActivity().runOnUiThread {
                        upgrading = false
                        binding.upgradeGroup.visibility = View.GONE
                        binding.tipGroup.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onMessageEvent(msg: EventBusMessage) {
        super.onMessageEvent(msg)
        val message = msg.message
        if (message is OtaMessage) {
            Log.i(TAG, "ota message=$message")
            if (message.tag == "serverVersion") {
                if (message.version.isNotEmpty()) {
                    checkVersion(true, message.version)
                } else {
                    checkVersion(false, getString(R.string.obtain_failed))
                }
            }
            if (message.tag == "otaInfo") {
                when (message.infoType) {
                    OTAInfoType.NO_NETWORK.type -> {
                        // 无网络
                        checkNetWork(false)
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            checkAddressVersion()
                        }
                    }

                    OTAInfoType.NO_OTA_VERSION.type -> {
                        // 无版本号
                        Log.i(TAG, "no version")
                        checkVersion(false, getString(R.string.obtain_failed))
                    }

                    OTAInfoType.UPGRADE_FAIL.type -> {
                        // 升级失败
                        upgrading = false
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.upgrade_failed),
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressbar.progress = 100
                        binding.stateBtn.text = getString(R.string.recheck)
                        stepHashMap[setp_download] = false
                    }

                    OTAInfoType.WITH_NETWORK.type -> {
                        // 有网络
                        checkNetWork(true)
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(2000)
                            checkAddressVersion()
                        }
                    }
                }
            }
        }
    }

    private fun checkVersion(versionState: Boolean, version: String) {
        requireActivity().runOnUiThread {
            Log.i(TAG, "versionState=$versionState")
            stepHashMap[setp_version] = versionState
            binding.latestVersion.text = version
            binding.latestVersion.setTextColor(
                if (versionState)
                    resources.getColor(R.color.ota_text_black_color, requireContext().theme)
                else resources.getColor(R.color.ota_text_red_color, requireContext().theme)
            )
            checkOtaState()
        }
    }

    private fun checkNetWork(netWorkState: Boolean) {
        requireActivity().runOnUiThread {
            Log.i(TAG, "netWorkState=$netWorkState")
            stepHashMap[setp_network] = netWorkState
            binding.checkNetTv.text =
                if (netWorkState) getString(R.string.speaker_network_connected)
                else getString(R.string.speaker_network_disconnected)
            binding.connectLoading.visibility = View.GONE
            binding.connectState.visibility = View.VISIBLE
            binding.connectState.setImageResource(
                if (netWorkState) R.mipmap.ic_success_state
                else R.mipmap.ic_error_state
            )
            binding.checkNetTv.setTextColor(
                if (netWorkState) resources.getColor(
                    R.color.ota_text_black_color,
                    requireContext().theme
                )
                else resources.getColor(R.color.ota_text_red_color, requireContext().theme)
            )
            checkOtaState()
        }
    }

    private fun checkBattery(battery: Int) {
        requireActivity().runOnUiThread {
            Log.i(TAG, "battery=$battery")
            val batteryState = battery >= 15
            stepHashMap[setp_battery] = batteryState
            binding.checkBatteryTv.text =
                if (batteryState) getString(R.string.battery_greater_than_15)
                else getString(R.string.battery_below_15)
            binding.batteryLoading.visibility = View.GONE
            binding.batteryState.visibility = View.VISIBLE
            binding.batteryState.setImageResource(
                if (batteryState) R.mipmap.ic_success_state
                else R.mipmap.ic_error_state
            )
            binding.checkBatteryTv.setTextColor(
                if (batteryState) resources.getColor(
                    R.color.ota_text_black_color,
                    requireContext().theme
                )
                else resources.getColor(R.color.ota_text_red_color, requireContext().theme)
            )
            checkOtaState()
        }
    }

    private fun checkOtaState() {
        if (!stepHashMap.containsKey(setp_battery) || !stepHashMap.containsKey(setp_network)
            || !stepHashMap.containsKey(setp_version)
        ) {
            Log.i(TAG, "Step not completed")
            return
        }
        binding.stateBtn.textSize = 16f
        if (stepHashMap[setp_battery] == false ||
            stepHashMap[setp_network] == false
        ) {
            binding.progressbar.progress = 0
            binding.stateBtn.text = getString(R.string.not_allow_upgrade)
            return
        }
        if (stepHashMap[setp_version] == false) {
            binding.progressbar.progress = 100
            binding.stateBtn.textSize = 14f
            binding.stateBtn.text = String.format(getString(R.string.consult_mail),e_mail)
            return
        }
        if (binding.localVersion.text.isEmpty()) {
            Log.i(TAG, "miss local version")
            return
        }
        if (binding.localVersion.text == binding.latestVersion.text) {
            binding.stateBtn.text = getString(R.string.already_latest_version)
            return
        }
        stepHashMap[setp_download] = true
        binding.progressbar.progress = 100
        binding.stateBtn.text = getString(R.string.start_upgrade)
    }

    private fun upgrade() {
        Log.i(TAG, "upgrade")
        if (upgrading) {
            return
        }
        upgrading = true
        binding.otaAttention.visibility = View.VISIBLE
        getAvailableURL()?.let {
            BleManager.getInstance().add(BleCMDUtil.sendOTAUrlAddress(it))
        }
    }

    private fun getAvailableURL(): String? {
        // 判断 address
        val localAddress = FastSharedPreferences.get(Configs.OTA_LOCAL_PATH)
            .getString(Configs.OTA_LOCAL_PATH, "")
        val serverAddress = FastSharedPreferences.get(Configs.OTA_SERVER_PATH)
            .getString(Configs.OTA_SERVER_PATH, "")
        Log.i(TAG, "localAddress=$localAddress,serverAddress=$serverAddress")
        if (localAddress == serverAddress) {
            return localAddress
        }
        var tempAddress = serverAddress
        val serverIsUrl = VerificationURLUtil.isValidUrl(serverAddress)
        Log.i(TAG, "serverIsUrl=$serverAddress")
        if (!serverIsUrl) {
            val localIsUrl = VerificationURLUtil.isValidUrl(localAddress)
            Log.i(TAG, "localIsUrl=$localIsUrl")
            if (localIsUrl) {
                tempAddress = localAddress
            }
        }
        return tempAddress
    }

    private var editDialog: Dialog? = null
    private fun showEditDialog() {
        if (editDialog != null) {
            editDialog?.show()
            return
        }
        editDialog = Dialog(requireContext(), R.style.custom_dialog)
        editDialog?.setContentView(R.layout.dialog_eidt_ota_tip)
        editDialog?.let { dialog ->
            val confirm: Button = dialog.findViewById(R.id.btn_confirm)
            val cancel: Button = dialog.findViewById(R.id.btn_cancel)
            val editText: EditText = dialog.findViewById(R.id.address_edit)
            cancel.setOnClickListener {
                dialog.dismiss()
            }
            confirm.setOnClickListener {
                if (editText.text.isEmpty()) {
                    Toast.makeText(
                        requireContext(), getString(R.string.edit_address),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                // 判断是否符合格式
                if (!isValidUrl(editText.text.toString())) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_address),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                editAddress = editText.text.toString()
                FastSharedPreferences.get(Configs.OTA_SERVER_PATH)
                    .edit().putString(Configs.OTA_SERVER_VERSION, editAddress).apply()
                initView()
                checkUpgrading()
                dialog.dismiss()
            }
            dialog.setCancelable(false)
            dialog.show()
        }
    }

    private fun isValidUrl(url: String): Boolean {
        val urlPattern = """^(https?|ftp)://[^\\s/$.?#].[^\\s]*$""".toRegex()
        return urlPattern.matches(url)
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO)

        intent.data = Uri.parse("mailto:$e_mail")// 设置邮件接收者
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(e_mail)) // 接收者邮箱
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.ota_address)) // 邮件主题
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_content)) // 邮件内容

//        intent.setDataAndType(Uri.parse("mailto:$e_mail") ,"text/plain")
//        startActivity(Intent.createChooser(intent, "Select your Email app"))
        // 检查是否有邮件客户端可以处理这个 Intent
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent) // 启动邮件客户端
        } else {
            // 没有可用的邮件客户端
            Log.i(TAG, "没有可用的邮箱客户端")
            Toast.makeText(requireContext(), getString(R.string.no_email_client), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BleManager.getInstance().removeBluetoothDataCallBack(TAG)
    }

    override fun getChildLayoutId(): Int = R.layout.fragment_oat
}