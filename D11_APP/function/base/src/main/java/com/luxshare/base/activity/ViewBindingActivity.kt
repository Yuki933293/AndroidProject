package com.luxshare.base.activity

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * Created by CaoYanYan
 * Date: 2024/3/12 10:07
 **/
abstract class ViewBindingActivity<VB : ViewBinding> : BaseActivity(),
    CoroutineScope by MainScope() {
    private lateinit var contentbinding: VB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentbinding = inflateViewBinding();
        setContentView(contentbinding.root)
    }

    protected fun getView(): View = contentbinding.root
    abstract fun inflateViewBinding(): VB
    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}