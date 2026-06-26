package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.db.PropertyDao
import com.example.real_estate_manager.data.db.PropertyEntity
import com.example.real_estate_manager.data.db.ReminderDao
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import com.example.real_estate_manager.reminders.ReminderTimeCalculator
import com.example.real_estate_manager.reminders.ReminderWorkScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderSyncPlannerTest {

    @Test
    fun serverEnabledReminderWithNullNextTrigger_recomputesFutureTrigger() = runBlocking {
        val now = LocalDateTime.of(2026, 6, 23, 10, 0)
        val rule = reminderRule(
            enabled = true,
            scheduleMode = ReminderScheduleMode.DAILY.name,
            hour = 10,
            minute = 2,
            nextTriggerAt = Long.MAX_VALUE
        )

        val normalized = ReminderSyncPlanner.normalizeRemoteRule(rule, now) { candidate, currentTime ->
            ReminderTimeCalculator.nextTriggerAt(candidate, currentTime)
        }

        assertTrue(normalized.enabled)
        assertNotEquals(Long.MAX_VALUE, normalized.nextTriggerAt)
        assertTrue(normalized.nextTriggerAt > now.toMillis())
    }

    @Test
    fun serverDisabledReminder_keepsUnscheduledState() = runBlocking {
        val rule = reminderRule(enabled = false, nextTriggerAt = 123_456L)

        val normalized = ReminderSyncPlanner.normalizeRemoteRule(rule) { _, _ ->
            error("Disabled reminders must not be recomputed")
        }

        assertFalse(normalized.enabled)
        assertEquals(Long.MAX_VALUE, normalized.nextTriggerAt)
    }

    @Test
    fun localIdReplacedByServerId_cancelsOldWorkAndSchedulesNewWork() = runBlocking {
        val reminderDao = FakeReminderDao()
        val scheduler = FakeReminderWorkScheduler()
        val repository = RoomReminderRepository(
            reminderDao = reminderDao,
            propertyDao = FakePropertyDao(),
            scheduler = scheduler
        )
        val now = System.currentTimeMillis()
        val localRule = reminderRule(id = "local-id", nextTriggerAt = now + 60_000L)
        val serverRule = localRule.copy(id = "server-id", nextTriggerAt = now + 120_000L, syncStatus = "SYNCED")

        repository.upsert(localRule)
        repository.deleteById(localRule.userId, localRule.id)
        repository.upsert(serverRule)

        assertNull(reminderDao.getById(localRule.userId, localRule.id))
        assertEquals(serverRule, reminderDao.getById(serverRule.userId, serverRule.id))
        assertTrue("old work should be cancelled", localRule.id in scheduler.cancelledRuleIds)
        assertFalse("old work should not remain scheduled", localRule.id in scheduler.scheduledRuleIds)
        assertTrue("new work should be scheduled", serverRule.id in scheduler.scheduledRuleIds)
    }

    private fun reminderRule(
        id: String = "rule-id",
        enabled: Boolean = true,
        scheduleMode: String = ReminderScheduleMode.DAILY.name,
        hour: Int = 10,
        minute: Int = 0,
        nextTriggerAt: Long = Long.MAX_VALUE
    ): ReminderRuleEntity =
        ReminderRuleEntity(
            id = id,
            userId = "user-id",
            propertyId = null,
            title = "Test reminder",
            message = null,
            type = "UTILITIES",
            scheduleMode = scheduleMode,
            oneTimeAt = null,
            rangeStartAt = null,
            rangeEndAt = null,
            dayOfMonth = null,
            rangeStartDay = null,
            rangeEndDay = null,
            repeatEveryDays = null,
            offsetDays = null,
            hour = hour,
            minute = minute,
            enabled = enabled,
            nextTriggerAt = nextTriggerAt,
            lastFiredAt = null,
            createdAt = 1L,
            updatedAt = 1L
        )

    private fun LocalDateTime.toMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private class FakeReminderDao : ReminderDao {
    private val reminders = linkedMapOf<String, ReminderRuleEntity>()

    override fun observeAll(userId: String): Flow<List<ReminderRuleEntity>> =
        flowOf(reminders.values.filter { it.userId == userId && it.syncStatus != "PENDING_DELETE" })

    override fun observeForProperty(userId: String, propertyId: String): Flow<List<ReminderRuleEntity>> =
        flowOf(reminders.values.filter { it.userId == userId && it.propertyId == propertyId && it.syncStatus != "PENDING_DELETE" })

    override suspend fun getDue(userId: String, nowMillis: Long): List<ReminderRuleEntity> =
        reminders.values.filter {
            it.userId == userId &&
                it.enabled &&
                it.nextTriggerAt <= nowMillis &&
                it.syncStatus != "PENDING_DELETE"
        }

    override suspend fun getById(userId: String, id: String): ReminderRuleEntity? =
        reminders[id]?.takeIf { it.userId == userId }

    override suspend fun upsert(rule: ReminderRuleEntity) {
        reminders[rule.id] = rule
    }

    override suspend fun upsertAll(rules: List<ReminderRuleEntity>) {
        rules.forEach { upsert(it) }
    }

    override suspend fun pending(userId: String): List<ReminderRuleEntity> =
        reminders.values.filter { it.userId == userId && it.syncStatus != "SYNCED" }

    override suspend fun ids(userId: String): List<String> =
        reminders.values.filter { it.userId == userId }.map { it.id }

    override suspend fun missingSyncedIds(userId: String, ids: List<String>): List<String> =
        reminders.values
            .filter { it.userId == userId && it.id !in ids && it.syncStatus == "SYNCED" }
            .map { it.id }

    override suspend fun deleteMissing(userId: String, ids: List<String>) {
        missingSyncedIds(userId, ids).forEach { reminders.remove(it) }
    }

    override suspend fun deleteAll(userId: String) {
        reminders.values.filter { it.userId == userId }.map { it.id }.forEach { reminders.remove(it) }
    }

    override suspend fun deleteById(userId: String, id: String) {
        reminders[id]?.takeIf { it.userId == userId }?.let { reminders.remove(id) }
    }
}

private class FakePropertyDao : PropertyDao {
    override fun list(userId: String): Flow<List<PropertyEntity>> = flowOf(emptyList())
    override suspend fun getById(userId: String, id: String): PropertyEntity? = null
    override suspend fun upsert(entity: PropertyEntity) = Unit
    override suspend fun upsertAll(entities: List<PropertyEntity>) = Unit
    override suspend fun deleteMissing(userId: String, ids: List<String>) = Unit
    override suspend fun deleteAll(userId: String) = Unit
    override suspend fun delete(userId: String, id: String) = Unit
    override suspend fun updateCover(userId: String, id: String, coverUri: String?) = Unit
}

private class FakeReminderWorkScheduler : ReminderWorkScheduler {
    val scheduledRuleIds = linkedSetOf<String>()
    val cancelledRuleIds = linkedSetOf<String>()

    override fun schedule() = Unit

    override fun scheduleRule(rule: ReminderRuleEntity) {
        if (!rule.enabled || rule.nextTriggerAt == Long.MAX_VALUE) {
            cancelRule(rule.id)
        } else {
            scheduledRuleIds += rule.id
        }
    }

    override fun cancelRule(ruleId: String) {
        cancelledRuleIds += ruleId
        scheduledRuleIds -= ruleId
    }
}
