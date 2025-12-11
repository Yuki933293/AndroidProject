package com.luxshare.bluetooth.thread

import android.util.Log
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue


/**
 * The write queue.
 *
 * @author ChenCe
 * @version version
 */
class WriteQueue<T> {
    private val TAG = this::class.java.simpleName
    private val blockingDeque: BlockingQueue<T> = LinkedBlockingQueue() // 缓冲区

    /**
     * 将数据加入队尾
     *
     * @param data data
     */
    fun add(data: T) {
        try {
            blockingDeque.put(data)
        } catch (e: InterruptedException) {
            Log.e(TAG, "add: ", e)
        }
    }

    /**
     * 将队头数据移出队列
     *
     * @return 返回被移出的数据
     */
    fun remove(): T? {
        var t: T? = null
        try {
            t = blockingDeque.take()
        } catch (e: InterruptedException) {
            Log.e(TAG, "remove: ", e)
        }
        return t
    }

    /**
     * 判断是否非空
     *
     * @return 返回是否非空
     */
    val isEmpty: Boolean
        get() = blockingDeque.isEmpty()
}