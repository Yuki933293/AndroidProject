package com.luxshare.base.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.luxshare.base.R


/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2024/12/26 14:37
 */
object ToastUtil {
    fun showToast(context: Context, message: String) {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val layout: View = inflater.inflate(R.layout.custom_toast, null)

        val text = layout.findViewById<TextView>(R.id.toast_text)
        text.text = message

        val toast = Toast(context)
        toast.setDuration(Toast.LENGTH_LONG)
        toast.view = layout
        toast.show()
    }
}