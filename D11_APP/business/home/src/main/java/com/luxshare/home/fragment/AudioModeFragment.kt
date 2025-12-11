package com.luxshare.home.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.alibaba.android.arouter.facade.annotation.Route
import com.luxshare.configs.Configs
import com.luxshare.configs.PathConfig
import com.luxshare.home.R
import org.greenrobot.eventbus.EventBus

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2024/12/24 9:07
 */
@Route(path = PathConfig.Path_AudioModeFragment)
class AudioModeFragment: BaseSpeakerFragment() {
    private lateinit var music_group: RelativeLayout

    private lateinit var music_btn: ImageView

    private lateinit var meeting_group: RelativeLayout

    private lateinit var meeting_btn: ImageView

    override fun getChildLayoutId(): Int {
        return R.layout.fragment_audio_mode
    }

    override fun initChildView(view: View) {
        music_group = view.findViewById(com.luxshare.resource.R.id.music_group)
        music_btn = view.findViewById(com.luxshare.resource.R.id.music_btn)
        meeting_group = view.findViewById(com.luxshare.resource.R.id.meeting_group)
        meeting_btn = view.findViewById(com.luxshare.resource.R.id.meeting_btn)

        music_group.setOnClickListener {
            if (music_btn.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            music_btn.visibility = View.VISIBLE
            meeting_btn.visibility = View.INVISIBLE
            sendAudioMode(Configs.OPERATE_MUSIC_MODE)
        }

        meeting_group.setOnClickListener {
            if (meeting_btn.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            meeting_btn.visibility = View.VISIBLE
            music_btn.visibility = View.INVISIBLE
            sendAudioMode(Configs.OPERATE_MEETING_MODE)
        }
    }

    override fun initData() {
        super.initData()
        val audioMode = arguments?.getInt("audio_mode", Configs.OPERATE_MEETING_MODE)
            ?: Configs.OPERATE_MEETING_MODE
        Log.i(TAG, "audio_mode=$audioMode")
        if (audioMode == Configs.OPERATE_MUSIC_MODE) {
            music_btn.visibility = View.VISIBLE
            meeting_btn.visibility = View.INVISIBLE
        } else {
            music_btn.visibility = View.INVISIBLE
            meeting_btn.visibility = View.VISIBLE
        }
    }

    private fun sendAudioMode(mode: Int) {
        val bundle = Bundle()
        bundle.putInt(Configs.KEY_AUDIO_MODE, mode)
        EventBus.getDefault().post(bundle)
    }

    override fun getBarTitle(): CharSequence {
        return requireContext().getString(R.string.music_meeting_mode)
    }
}