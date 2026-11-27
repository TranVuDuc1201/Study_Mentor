package com.example.study_mentor.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Notifications - Phan 1
 * Backend gui thong bao cuc bo cho Study Mentor.
 */
object StudyNotificationHelper {

    private const val CHANNEL_ID = "study_mentor_notifications"
    private const val CHANNEL_NAME = "Study Mentor Notifications"
    private const val NOTIFICATION_ID = 1001

    // Tao kenh thong bao cho Android 8.0 tro len.
    fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thong bao hoc tap tu Study Mentor"
            }

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    // Kiem tra quyen thong bao.
    fun hasNotificationPermission(context: Context): Boolean {

        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // Gui thong bao thu nghiem.
    // Tra ve false neu chua co quyen thong bao.
    fun showTestNotification(context: Context): Boolean {

        if (!hasNotificationPermission(context)) {
            return false
        }

        createNotificationChannel(context)

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                Notification.Builder(context)
            }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Study Mentor")
            .setContentText("Den gio hoc tap roi! Hay bat dau nhe.")
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)

        return true
    }
}
