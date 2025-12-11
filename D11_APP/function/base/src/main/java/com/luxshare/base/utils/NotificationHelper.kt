package com.luxshare.base.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.luxshare.base.R

/**
 * @desc 功能描述
 *
 * @author hudebo
 * @date  2025/1/15 11:27
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Default Channel"
        private const val CHANNEL_DESCRIPTION = "This is the default channel for notifications."
        private const val NOTIFICATION_ID = 1
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableLights(true) // LED 灯是否应该闪烁
            lightColor = Color.RED // LED 灯闪烁的颜色
            enableVibration(true) // 设备是否应该振动
            vibrationPattern = longArrayOf(0, 1000, 500, 1000) // 设备振动的模式 表示设备将立即开始振动 1000 毫秒，然后静默 500 毫秒，再次振动 1000 毫秒
            setShowBadge(true) // 通常用于指定是否应该在应用图标上显示未读通知的徽章（或称为角标）
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun showNotification(title: String, message: String) {
//        val intent = Intent(context, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//        val pendingIntent = PendingIntent.getActivity(
//            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
//        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
            .setContentText(message)
//            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}