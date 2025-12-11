package com.luxshare.home.fragment

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import com.alibaba.android.arouter.facade.annotation.Route
import com.luxshare.configs.PathConfig
import com.luxshare.resource.R

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2024/12/23 15:34
 */
@Route(path = PathConfig.Path_SoundLocationFragment)
class SoundLocationFragment : BaseSpeakerFragment() {
    private lateinit var openImg: ImageView

    private lateinit var closeImg: ImageView

    private lateinit var close_group: RelativeLayout

    override fun getChildLayoutId(): Int {
        return R.layout.fragment_sound_location
    }

    override fun initChildView(view: View) {
        openImg = view.findViewById(R.id.open)
        close_group = view.findViewById(R.id.close_group)
        closeImg = view.findViewById(R.id.close)

        close_group.setOnClickListener {
            Toast.makeText(requireContext(), R.string.not_supported, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getBarTitle(): CharSequence {
        return requireContext().getString(R.string.sound_location)
    }
}