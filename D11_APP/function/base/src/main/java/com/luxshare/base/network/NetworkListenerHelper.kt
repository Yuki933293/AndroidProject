package com.luxshare.base.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import android.os.Build
import android.util.Log
import com.luxshare.base.network.NetworkUtils.getNetWorkState
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @desc 网络状态变化的监听类，根据android不同版本的系统，有 [ConnectivityManager.registerNetworkCallback]和注册广播两种实现方式；
 * @author hudebo
 * @date 2023/10/24
 */
@SuppressLint("StaticFieldLeak")
object NetworkListenerHelper {
    private val TAG = "NetworkListenerHelper"
    private var mContext: Context? = null

    @Volatile
    private var mListenerList: CopyOnWriteArrayList<NetworkConnectedListener>? = null

    /**
     * 注册网络状态的监听；
     */
    @SuppressLint("MissingPermission")
    fun registerNetworkListener() {
        val connectivityManager =
            mContext!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(MyNetworkCallback(connectivityManager))
    }

    /**
     * 通知所有接收者；
     *
     * @param isConnected
     * @param networkStatus
     */
    private fun notifyAllListeners(
        isConnected: Boolean,
        networkStatus: NetworkStatus,
        linkProperties: LinkProperties?
    ) {
        mListenerList?.let {
            for (listener in it) {
                listener?.onNetworkConnected(isConnected, networkStatus, linkProperties)
            }
        }
    }



    /**
     * 添加回调的监听者；
     */
    @Synchronized
    fun addListener(listener: NetworkConnectedListener?) {
        if (listener == null) {
            return
        }
        if (mListenerList == null) {
            mListenerList = CopyOnWriteArrayList()
        }
        // 防止重复添加；
        if (!mListenerList!!.contains(listener)) {
            mListenerList!!.add(listener)
        }
    }

    /**
     * 移除某个回调实例；
     *
     * @param listener
     */
    @Synchronized
    fun removeListener(listener: NetworkConnectedListener?) {
        mListenerList?.let {
            if (listener != null) {
                it.remove(listener)
            }
        }
    }

    fun unregisterNetworkCallback() {
        if (mContext == null) {
            return
        }
        val connectivityManager = mContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager == null) {
            Log.e(
                TAG,
                "registerNetworkListener#return#connectivityManager=$connectivityManager"
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.unregisterNetworkCallback(NetworkCallback())
        }
    }

    interface NetworkConnectedListener {
        /**
         * @param isConnected
         * @param networkStatus
         */
        fun onNetworkConnected(
            isConnected: Boolean,
            networkStatus: NetworkStatus?,
            linkProperties: LinkProperties?
        )
    }

    @SuppressLint("NewApi")
    private class MyNetworkCallback(val connectivityManager: ConnectivityManager) : NetworkCallback() {
        //当用户与网络连接（或断开连接）（可以是WiFi或蜂窝网络）时，这两个功能均作为默认回调;
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "onAvailable#network=$network")
            // 需要同步获取一次网络状态；
            val linkProperties = connectivityManager.getLinkProperties(network)

            val netWorkState = getNetWorkState(mContext!!)
            Log.d(TAG, "onAvailable#netWorkState=$netWorkState")
            //
            notifyAllListeners(true, netWorkState, linkProperties)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "onLost#network=$network")
            // 需要同步获取一次网络状态；
            val netWorkState = getNetWorkState(mContext!!)
            Log.d(TAG, "onLost#netWorkState=$netWorkState")
            val linkProperties = connectivityManager.getLinkProperties(network)
            //
            notifyAllListeners(false, netWorkState, linkProperties)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            Log.d(TAG, "onCapabilitiesChanged#network=$network")
            //            Log.d(TAG, "onCapabilitiesChanged#network=" + network + ", networkCapabilities=" + networkCapabilities);
            // 表示能够和互联网通信（这个为true表示能够上网）
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        Log.d(TAG, "onCapabilitiesChanged#网络类型为wifi")
                    }
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        Log.d(TAG, "onCapabilitiesChanged#蜂窝网络")
                    }
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        Log.d(TAG, "onCapabilitiesChanged#以太网")
                    }
                    else -> {
                        Log.d(TAG, "onCapabilitiesChanged#其他网络")
                    }
                }
            }
        }
    }

    fun init(context: Context): NetworkListenerHelper {
        mContext = context
        return this
    }
}