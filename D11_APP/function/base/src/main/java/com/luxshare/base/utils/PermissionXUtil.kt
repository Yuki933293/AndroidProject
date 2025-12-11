package com.luxshare.base.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX

/**
 * @desc
 * @author hudebo
 * @date 2023/10/20
 */
object PermissionXUtil {
    fun applyPermisions(
        context: FragmentActivity,
        permissions: List<String>,
        lisenter: IPermissionLisenter
    ) {
        PermissionX.init(context)
            .permissions(permissions)
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(deniedList, "即将重新申请的权限是程序必须依赖的权限", "接受", "取消")
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(deniedList, "您需要去应用程序设置当中手动开启权限", "接受", "取消")
            }
            .request { allGranted: Boolean, grantedList: List<String?>?, deniedList: List<String?>? ->
                if (allGranted) {
                    lisenter.allGranted()
                } else {
                    Log.e("PermissionXUtil", "These permissions are denied: $deniedList")
                    lisenter.denied(deniedList)
                }
            }
    }

    /**
     * 检查其他权限：所有文件管理权限
     *
     *  @param activity activity
     */
    fun applyFileManagerPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.i("PermissionXUtil", "external=${Environment.isExternalStorageManager()}")
            if (!Environment.isExternalStorageManager()) { // 是否有访问所有文件的权限
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            }
        }
    }
}