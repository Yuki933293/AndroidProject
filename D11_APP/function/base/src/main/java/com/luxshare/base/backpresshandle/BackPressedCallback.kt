package com.luxshare.home.backpresshandle


import androidx.annotation.MainThread
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 返回键事件回调
 *
 * @author Caoyanyan
 */

abstract class BackPressedCallback {

    private val mCancellables = CopyOnWriteArrayList<Cancellable>()

    /**
     * 默认没有取令牌，不能执行回调
     */
    public var isGranted = false

    fun setToken(isGranted: Boolean) {
        this.isGranted = isGranted
    }

    /**
     * 实现此方法处理返回事件
     * @return true:消耗了返回事件 ，false：不消耗返回事件
     */
    @MainThread
    abstract fun handleOnBackPressed(owner: BackPressedOwner): Boolean

    /**
     * 从[BackPressedDispatcher]中移除自己
     */
    @MainThread
    fun remove() {
        mCancellables.forEach {
            it.cancel()
        }
    }

    internal fun addCancellable(cancellable: Cancellable) {
        mCancellables.add(cancellable)
    }

    internal fun removeCancellable(cancellable: Cancellable) {
        mCancellables.remove(cancellable)
    }

}

