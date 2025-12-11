package com.luxshare.base.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * @desc 功能描述
 * @author hudebo
 * @date 2023/10/24
 */
object NetworkUtils {
    @JvmStatic
    @SuppressLint("MissingPermission")
    fun getNetWorkState(context: Context): NetworkStatus {
        return when {
            isEthernetConnected(context) -> {
                NetworkStatus.ETHERNET
            }
            isMobileConnected(context) -> {
                NetworkStatus.MOBILE
            }
            isWifiConnected(context) -> {
                NetworkStatus.WIFI
            }
            else -> {
                NetworkStatus.NONE
            }
        }
    }

    /**
     * 判断是否是以太网
     */
    fun isEthernetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false

            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            val networks = connectivityManager.allNetworks
            for (network in networks) {
                val networkInfo = connectivityManager.getNetworkInfo(network)
                if (networkInfo?.type == ConnectivityManager.TYPE_ETHERNET && networkInfo.isConnected) {
                    return true
                }
            }
        }

        return false
    }


    /**
     * 判断网络是否连接
     *
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    fun isOnline(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val connMgr =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connMgr.activeNetworkInfo
            return networkInfo != null && networkInfo.isAvailable
        } else {
            val connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities != null) {
                return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun isWifiConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            if (networkInfo != null) {
                return networkInfo.isAvailable
            }
        } else {
            val connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities != null) {
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    fun isMobileConnected(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            if (networkInfo != null) {
                return networkInfo.isAvailable
            }
        } else {
            val connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities != null) {
                return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
        }
        return false
    }
}