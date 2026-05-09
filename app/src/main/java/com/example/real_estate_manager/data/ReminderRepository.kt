package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import com.example.real_estate_manager.data.model.ReminderType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface ReminderRepository {
    fun observeAll(userId: String): Flow<List<ReminderRuleEntity>>
    fun observeForProperty(userId: String, propertyId: String): Flow<List<ReminderRuleEntity>>
    suspend fun getDue(userId: String, nowMillis: Long): List<ReminderRuleEntity>
    suspend fun upsert(rule: ReminderRuleEntity)
    suspend fun deleteById(userId: String, id: String)
    suspend fun createRule(
        userId: String,
        propertyId: String?,
        title: String,
        message: String?,
        type: ReminderType,
        scheduleMode: ReminderScheduleMode,
        oneTimeAt: Long?,
        rangeStartAt: Long?,
        rangeEndAt: Long?,
        dayOfMonth: Int?,
        rangeStartDay: Int?,
        rangeEndDay: Int?,
        repeatEveryDays: Int?,
        offsetDays: Int?,
        hour: Int,
        minute: Int,
        enabled: Boolean
    )
    suspend fun createDefaultLeaseEndReminders(userId: String, propertyId: String)
    suspend fun recomputeNextTriggerAt(rule: ReminderRuleEntity, now: LocalDateTime = LocalDateTime.now()): Long
}
