package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.db.PropertyDao
import com.example.real_estate_manager.data.db.ReminderDao
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import com.example.real_estate_manager.data.model.ReminderType
import com.example.real_estate_manager.reminders.ReminderWorkScheduler
import com.example.real_estate_manager.reminders.ReminderTimeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val propertyDao: PropertyDao,
    private val scheduler: ReminderWorkScheduler
) : ReminderRepository {

    override fun observeAll(userId: String): Flow<List<ReminderRuleEntity>> =
        reminderDao.observeAll(userId)

    override fun observeForProperty(userId: String, propertyId: String): Flow<List<ReminderRuleEntity>> =
        reminderDao.observeForProperty(userId, propertyId)

    override suspend fun getDue(userId: String, nowMillis: Long): List<ReminderRuleEntity> =
        withContext(Dispatchers.IO) { reminderDao.getDue(userId, nowMillis) }

    override suspend fun upsert(rule: ReminderRuleEntity) =
        withContext(Dispatchers.IO) {
            reminderDao.upsert(rule)
            scheduler.scheduleRule(rule)
        }

    override suspend fun deleteById(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            reminderDao.deleteById(userId, id)
            scheduler.cancelRule(id)
        }

    override suspend fun createRule(
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
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val draft = ReminderRuleEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            propertyId = propertyId,
            title = title,
            message = message,
            type = type.name,
            scheduleMode = scheduleMode.name,
            oneTimeAt = oneTimeAt,
            rangeStartAt = rangeStartAt,
            rangeEndAt = rangeEndAt,
            dayOfMonth = dayOfMonth,
            rangeStartDay = rangeStartDay,
            rangeEndDay = rangeEndDay,
            repeatEveryDays = repeatEveryDays,
            offsetDays = offsetDays,
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            enabled = enabled,
            nextTriggerAt = Long.MAX_VALUE,
            lastFiredAt = null,
            createdAt = now,
            updatedAt = now
        )
        val rule = draft.copy(nextTriggerAt = recomputeNextTriggerAt(draft))
        reminderDao.upsert(rule)
        scheduler.scheduleRule(rule)
    }

    override suspend fun createDefaultLeaseEndReminders(userId: String, propertyId: String) {
        listOf(60, 30, 7).forEach { offset ->
            createRule(
                userId = userId,
                propertyId = propertyId,
                title = "Скоро окончание аренды",
                message = "До окончания аренды осталось $offset дней",
                type = ReminderType.LEASE_END,
                scheduleMode = ReminderScheduleMode.RELATIVE_TO_LEASE_END,
                oneTimeAt = null,
                rangeStartAt = null,
                rangeEndAt = null,
                dayOfMonth = null,
                rangeStartDay = null,
                rangeEndDay = null,
                repeatEveryDays = null,
                offsetDays = offset,
                hour = 14,
                minute = 0,
                enabled = true
            )
        }
    }

    override suspend fun recomputeNextTriggerAt(rule: ReminderRuleEntity, now: LocalDateTime): Long =
        withContext(Dispatchers.IO) {
            if (!rule.enabled) return@withContext Long.MAX_VALUE
            when (enumValueOf<ReminderScheduleMode>(rule.scheduleMode)) {
                ReminderScheduleMode.ONE_TIME,
                ReminderScheduleMode.DAILY,
                ReminderScheduleMode.MONTHLY,
                ReminderScheduleMode.DATE_RANGE,
                ReminderScheduleMode.DAY_OF_MONTH,
                ReminderScheduleMode.DATE_RANGE_MONTHLY -> ReminderTimeCalculator.nextTriggerAt(rule, now)
                ReminderScheduleMode.RELATIVE_TO_LEASE_END -> nextLeaseEnd(rule, now)
            }
        }

    private suspend fun nextLeaseEnd(rule: ReminderRuleEntity, now: LocalDateTime): Long {
        val propertyId = rule.propertyId ?: return Long.MAX_VALUE
        val leaseTo = propertyDao.getById(rule.userId, propertyId)?.leaseTo
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return Long.MAX_VALUE
        val candidate = atRuleTime(leaseTo.minusDays((rule.offsetDays ?: 0).toLong()), rule)
        return if (candidate.isAfter(now)) candidate.toMillis() else Long.MAX_VALUE
    }

    private fun atRuleTime(date: LocalDate, rule: ReminderRuleEntity): LocalDateTime =
        date.atTime(rule.hour.coerceIn(0, 23), rule.minute.coerceIn(0, 59))

    private fun LocalDateTime.toMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
