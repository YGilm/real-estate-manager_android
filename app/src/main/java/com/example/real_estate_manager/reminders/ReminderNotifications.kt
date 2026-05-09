package com.example.real_estate_manager.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ReminderNotifications {
    const val CHANNEL_ID = "channel_reminders_general"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Локальные напоминания по объектам недвижимости"
        }
        manager.createNotificationChannel(channel)
    }
}
