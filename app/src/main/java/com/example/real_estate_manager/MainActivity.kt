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

    private val openNotificationsRequest = mutableStateOf(false)

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
                openNotificationsRequest = openNotificationsRequest.value,
                onNotificationsRequestHandled = { openNotificationsRequest.value = false }
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
        if (intent?.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false) == true ||
            intent?.action == ACTION_OPEN_NOTIFICATIONS
        ) {
            openNotificationsRequest.value = true
        }
    }

    companion object {
        const val ACTION_OPEN_NOTIFICATIONS = "com.example.real_estate_manager.OPEN_NOTIFICATIONS"
        const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
}
