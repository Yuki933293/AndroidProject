package com.luxshare.base.utils

import android.content.Context

/**
 * Created by CaoYanYan
 * Date: 2023/11/1 15:36
 **/
 class DisplayUtil {

    companion object {

        /**
         * screenWidth:640,screenHeight:480
         * 当前只用于判断是否是穿戴设备
         */
        fun isWearDevice(context: Context): Boolean {
            return context.resources.displayMetrics.widthPixels > context.resources.displayMetrics.heightPixels
        }

        /**
         * 获取屏幕宽度
         */
        fun getScreenWidth(context: Context): Int {
            return context.resources.displayMetrics.widthPixels
        }

        /**
         * 获取屏幕高度
         */
        fun getScreenHeight(context: Context): Int {
            return context.resources.displayMetrics.heightPixels
        }

        /**
         * 获取屏幕分辨率
         */
        fun getScreenRatio(context: Context): String {
            return getScreenWidth(context).toString() + "X" + getScreenHeight(context).toString()
        }

        /**
         * dp转px
         */
        fun dip2px(appContext: Context, dipValue: Float): Int {
            val scale = appContext.resources.displayMetrics.density
            return (dipValue * scale + 0.5f).toInt()
        }

        /**
         * px转dp
         */
        fun px2dip(appContext: Context, pxValue: Float): Int {
            val scale = appContext.resources.displayMetrics.density
            return (pxValue / scale + 0.5f).toInt()
        }
    }
}
