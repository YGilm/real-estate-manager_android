package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.db.ReminderRuleEntity
import java.time.LocalDateTime

object ReminderSyncPlanner {
    suspend fun normalizeRemoteRule(
        rule: ReminderRuleEntity,
        now: LocalDateTime = LocalDateTime.now(),
        recomputeNextTriggerAt: suspend (ReminderRuleEntity, LocalDateTime) -> Long
    ): ReminderRuleEntity {
        if (!rule.enabled) {
            return rule.copy(nextTriggerAt = Long.MAX_VALUE)
        }

        val next = recomputeNextTriggerAt(rule, now)
        return if (next == Long.MAX_VALUE) {
            rule.copy(enabled = false, nextTriggerAt = Long.MAX_VALUE)
        } else {
            rule.copy(nextTriggerAt = next)
        }
    }
}
