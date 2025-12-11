package com.luxshare.base.network


/**
 * @desc 网络连接状态的枚举
 * @author hudebo
 * @date 2023/10/24
 */
enum class NetworkStatus(val code: Int, val msg: String) {
    /**
     * ；
     */
    NONE(-1, "无网络连接"),
    /**
     * 手机网络
     */
    MOBILE(0, "移动网络连接"),
    /**
     * wifi
     */
    WIFI(1, "WIFI连接"),

    /**
     * 以太网
     */
    ETHERNET(2, "以太网连接");

    override fun toString(): String {
        return "NetwordStatus{" +
                "status=" + code +
                ", desc='" + msg + '\'' +
                "} " + super.toString();
    }
}