package com.luxshare.base.xlog

import com.elvishew.xlog.XLog

/**
 * 扩展Xlog的功能
 * Created by CaoYanYan
 * Date: 2023/12/7 9:29
 **/
object XLogExt {
    public fun v(Tag: String, msg: String?) {
        XLog.v("$Tag:$msg")
    }

    public fun d(Tag: String, msg: String?) {
        XLog.d("$Tag:$msg")
    }

    public fun i(Tag: String, msg: String?) {
        XLog.i("$Tag:$msg")
    }

    public fun e(Tag: String, msg: String?) {
        XLog.e("$Tag:$msg")
    }
}
