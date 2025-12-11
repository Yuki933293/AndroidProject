package com.luxshare.base.utils

import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings


/**
 * Created by CaoYanYan
 * Date: 2023/3/16 16:07
 **/
object FactoryToolUtil {
    /**
     * 判断是否开启了GPS或网络定位开关
     */
    fun isLocationProviderEnabled(context: Context?, isSupportOhter: Boolean): Boolean {
        var result = false
        val locationManager = context!!.applicationContext
            .getSystemService(LOCATION_SERVICE) as LocationManager ?: return false
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            result = true
        }
        if (!result && isSupportOhter && locationManager
                .isProviderEnabled(LocationManager.FUSED_PROVIDER)
        ) {
            result = true
        }
        return result
    }

    fun isLocationProviderEnabled(context: Context?): Boolean {
        return isLocationProviderEnabled(context, false)
    }

    /**
     * 跳转到设置界面，引导用户开启定位服务
     */
    fun openLocationServer(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
        context.startActivity(intent)
    }

}