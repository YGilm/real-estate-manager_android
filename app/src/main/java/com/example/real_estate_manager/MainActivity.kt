package com.example.real_estate_manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.real_estate_manager.auth.UserSession
import com.example.real_estate_manager.navigation.NotificationNavigationTarget
import com.example.real_estate_manager.reminders.ReminderNotifications
import com.example.real_estate_manager.reminders.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var userSession: UserSession

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val notificationNavigationTarget = mutableStateOf<NotificationNavigationTarget?>(null)

    private val appLifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                // Приложение вернулось на передний план
                lifecycleScope.launch {
                    userSession.onAppForeground()
                }
            }
            Lifecycle.Event.ON_STOP -> {
                // Приложение ушло в фон
                lifecycleScope.launch {
                    userSession.onAppBackground()
                }
            }
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNotifications.ensureChannels(this)
        reminderScheduler.schedule()

        // На всякий случай сразу проверим TTL при старте
        lifecycleScope.launch {
            userSession.onAppForeground()
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        handleIntent(intent)

        setContent {
            RealEstateApp(
                notificationNavigationTarget = notificationNavigationTarget.value,
                onNotificationNavigationHandled = { notificationNavigationTarget.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
        super.onDestroy()
    }

    private fun handleIntent(intent: Intent?) {
        val target = notificationTargetFrom(intent)
        if (target != null) {
            notificationNavigationTarget.value = target
        }
    }

    companion object {
        const val ACTION_OPEN_NOTIFICATIONS = "com.example.real_estate_manager.OPEN_NOTIFICATIONS"
        const val ACTION_OPEN_NOTIFICATION_DETAILS = "com.example.real_estate_manager.OPEN_NOTIFICATION_DETAILS"
        const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_REMINDER_ID = "reminder_id"

        fun notificationTargetFrom(intent: Intent?): NotificationNavigationTarget? =
            when {
                intent?.action == ACTION_OPEN_NOTIFICATION_DETAILS -> NotificationNavigationTarget(
                    notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID),
                    reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
                )
                intent?.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false) == true ||
                    intent?.action == ACTION_OPEN_NOTIFICATIONS -> NotificationNavigationTarget(
                        notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID),
                        reminderId = intent.getStringExtra(EXTRA_REMINDER_ID)
                    )
                else -> null
            }
    }
}
