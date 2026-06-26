package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import com.example.real_estate_manager.network.mappers.toRequestDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class ReminderSnoozePlannerTest {

    @Test
    fun snoozeCreatesFutureTriggerAndEnablesReminder() {
        val now = 1_000_000L
        val snoozed = ReminderSnoozePlanner.snooze(
            rule = reminderRule(enabled = false, nextTriggerAt = Long.MAX_VALUE),
            nowMillis = now,
            delayMillis = 10 * 60_000L
        )

        assertTrue(snoozed.enabled)
        assertEquals(now + 10 * 60_000L, snoozed.nextTriggerAt)
        assertEquals(now, snoozed.updatedAt)
    }

    @Test
    fun oneTimeSnoozeUpdatesSerializedOneTimeAtToFutureTrigger() {
        val now = System.currentTimeMillis()
        val triggerAt = now + 10 * 60_000L
        val snoozed = ReminderSnoozePlanner.snooze(
            rule = reminderRule(
                enabled = true,
                nextTriggerAt = now - 1,
                oneTimeAt = now - 1
            ),
            nowMillis = now,
            delayMillis = 10 * 60_000L
        )

        val request = snoozed.toRequestDto()

        assertEquals(triggerAt, snoozed.oneTimeAt)
        assertEquals(triggerAt, snoozed.nextTriggerAt)
        assertNotNull(request.oneTimeAt)
        assertTrue(OffsetDateTime.parse(request.oneTimeAt).toInstant().toEpochMilli() >= triggerAt)
        assertEquals(triggerAt, request.nextTriggerAt)
    }

    @Test
    fun recurringSnoozeDoesNotSerializeStaleOneTimeAt() {
        val now = System.currentTimeMillis()
        val snoozed = ReminderSnoozePlanner.snooze(
            rule = reminderRule(
                enabled = true,
                scheduleMode = ReminderScheduleMode.DAILY.name,
                nextTriggerAt = now - 1,
                oneTimeAt = now - 1
            ),
            nowMillis = now,
            delayMillis = 10 * 60_000L
        )

        val request = snoozed.toRequestDto()

        assertNull(snoozed.oneTimeAt)
        assertNull(request.oneTimeAt)
        assertEquals(now + 10 * 60_000L, request.nextTriggerAt)
    }

    private fun reminderRule(
        enabled: Boolean,
        nextTriggerAt: Long,
        scheduleMode: String = ReminderScheduleMode.ONE_TIME.name,
        oneTimeAt: Long? = null
    ): ReminderRuleEntity =
        ReminderRuleEntity(
            id = "reminder-id",
            userId = "user-id",
            propertyId = null,
            title = "Reminder",
            message = null,
            type = "UTILITIES",
            scheduleMode = scheduleMode,
            oneTimeAt = oneTimeAt,
            rangeStartAt = null,
            rangeEndAt = null,
            dayOfMonth = null,
            rangeStartDay = null,
            rangeEndDay = null,
            repeatEveryDays = null,
            offsetDays = null,
            hour = 10,
            minute = 0,
            enabled = enabled,
            nextTriggerAt = nextTriggerAt,
            lastFiredAt = null,
            createdAt = 1L,
            updatedAt = 1L
        )
}
