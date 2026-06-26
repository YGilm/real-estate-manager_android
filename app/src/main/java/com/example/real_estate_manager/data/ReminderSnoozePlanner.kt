package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import java.time.Instant
import java.time.ZoneId

object ReminderSnoozePlanner {
    fun snooze(rule: ReminderRuleEntity, nowMillis: Long, delayMillis: Long): ReminderRuleEntity {
        val triggerAt = nowMillis + delayMillis.coerceAtLeast(1L)
        val base = rule.copy(
            enabled = true,
            nextTriggerAt = triggerAt,
            updatedAt = nowMillis
        )
        return if (rule.scheduleMode == ReminderScheduleMode.ONE_TIME.name) {
            val dateTime = Instant.ofEpochMilli(triggerAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
            base.copy(
                oneTimeAt = triggerAt,
                hour = dateTime.hour,
                minute = dateTime.minute
            )
        } else {
            base.copy(oneTimeAt = null)
        }
    }
}
