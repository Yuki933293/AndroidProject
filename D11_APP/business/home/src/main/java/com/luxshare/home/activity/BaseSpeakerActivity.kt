package com.luxshare.home.activity

import android.os.Bundle
import android.os.Handler
import com.luxshare.base.activity.BaseActivity
import com.luxshare.ble.eventbus.EventBusMessage
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2024/12/21 15:12
 */
abstract class BaseSpeakerActivity: BaseActivity() {
    private val TAG: String = this::class.java.simpleName
    protected lateinit var mHandler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        mHandler = Handler(mainLooper)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(msg: EventBusMessage) {

    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}