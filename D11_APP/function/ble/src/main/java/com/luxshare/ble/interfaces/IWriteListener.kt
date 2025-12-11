package com.luxshare.ble.interfaces

/**
 * The bluetooth write listener.
 *
 * @author ChenCe
 * @version version
 */
interface IWriteListener {
    fun onWriteSuccess() // 写成功
    fun onWriteFailure() // 写失败
}
