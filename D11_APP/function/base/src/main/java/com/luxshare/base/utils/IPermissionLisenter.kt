package com.luxshare.base.utils

interface IPermissionLisenter {
    fun allGranted()
    fun denied(deniedList: List<String?>?)
}