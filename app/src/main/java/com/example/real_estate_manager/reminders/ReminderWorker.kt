package com.example.real_estate_manager.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.real_estate_manager.R
import com.example.real_estate_manager.auth.UserSession
import com.example.real_estate_manager.data.ReminderRepository
import com.example.real_estate_manager.data.db.PropertyDao
import com.example.real_estate_manager.data.db.ReminderDao
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val session: UserSession,
    private val reminderRepository: ReminderRepository,
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler,
    private val propertyDao: PropertyDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ReminderNotifications.ensureChannels(applicationContext)
        val userId = inputData.getString(KEY_USER_ID) ?: session.userIdFlow.first() ?: return Result.success()
        val nowMillis = System.currentTimeMillis()
        val ruleId = inputData.getString(KEY_RULE_ID)
        val dueRules = if (ruleId == null) {
            reminderRepository.getDue(userId, nowMillis)
        } else {
            val rule = reminderDao.getById(userId, ruleId)
            when {
                rule == null || !rule.enabled -> emptyList()
                rule.nextTriggerAt > nowMillis -> {
                    Log.i(TAG, "Worker started early for rule=$ruleId; rescheduling")
                    scheduler.scheduleRule(rule)
                    return Result.success()
                }
                else -> listOf(rule)
            }
        }
        dueRules.forEach { rule ->
            Log.i(TAG, "Showing reminder notification rule=${rule.id}")
            try {
                showNotification(rule)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to show reminder notification rule=${rule.id}", t)
                return Result.failure()
            }
            val firedAt = System.currentTimeMillis()
            val next = if (
                rule.scheduleMode == ReminderScheduleMode.RELATIVE_TO_LEASE_END.name ||
                rule.scheduleMode == ReminderScheduleMode.ONE_TIME.name
            ) {
                Long.MAX_VALUE
            } else {
                reminderRepository.recomputeNextTriggerAt(rule, LocalDateTime.now())
            }
            val updated = rule.copy(
                lastFiredAt = firedAt,
                nextTriggerAt = next,
                updatedAt = firedAt,
                enabled = next != Long.MAX_VALUE
            )
            reminderRepository.upsert(updated)
            scheduler.scheduleRule(updated)
        }
        return Result.success()
    }

    private suspend fun showNotification(rule: ReminderRuleEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "POST_NOTIFICATIONS is not granted")
            return
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val propertyName = rule.propertyId
            ?.let { propertyDao.getById(rule.userId, it)?.name }
        val title = rule.title.ifBlank { "Напоминание" }
        val message = rule.message?.takeIf { it.isNotBlank() }
        val propertyLine = propertyName?.let { "Объект: $it" } ?: "Объект: без привязки"
        val text = message ?: propertyLine
        val details = buildString {
            append(propertyLine)
            if (!message.isNullOrBlank()) {
                append("\n")
                append(message)
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(propertyName ?: "Без привязки")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(details)
                    .setBigContentTitle(title)
                    .setSummaryText(propertyName ?: "Без привязки")
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(rule.id.hashCode(), notification)
        Log.i(TAG, "Notification posted rule=${rule.id}")
    }

    companion object {
        private const val TAG = "ReminderWorker"
        const val KEY_USER_ID = "userId"
        const val KEY_RULE_ID = "ruleId"
    }
}
