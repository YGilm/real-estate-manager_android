package com.example.real_estate_manager.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleRule(rule: ReminderRuleEntity) {
        if (!rule.enabled || rule.nextTriggerAt == Long.MAX_VALUE) {
            WorkManager.getInstance(context).cancelUniqueWork(ruleWorkName(rule.id))
            return
        }
        val delay = (rule.nextTriggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putString(ReminderWorker.KEY_USER_ID, rule.userId)
                    .putString(ReminderWorker.KEY_RULE_ID, rule.id)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ruleWorkName(rule.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelRule(ruleId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(ruleWorkName(ruleId))
    }

    companion object {
        const val WORK_NAME = "reminder_scheduler"
        fun ruleWorkName(ruleId: String): String = "reminder_$ruleId"
    }
}
