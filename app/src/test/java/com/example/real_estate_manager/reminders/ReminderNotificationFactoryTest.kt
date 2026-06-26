package com.example.real_estate_manager.reminders

import com.example.real_estate_manager.data.db.NotificationRelatedEntityType
import com.example.real_estate_manager.data.db.NotificationType
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderNotificationFactoryTest {

    @Test
    fun reminderFired_createsPersistentUnreadNotificationEntity() {
        val rule = reminderRule()

        val entity = ReminderNotificationFactory.createEntity(
            id = "notification-id",
            rule = rule,
            propertyName = "Main flat",
            createdAt = 1_000L
        )

        assertEquals("notification-id", entity.id)
        assertEquals(NotificationType.REMINDER.name, entity.type)
        assertEquals(NotificationRelatedEntityType.REMINDER.name, entity.relatedEntityType)
        assertEquals(rule.id, entity.relatedEntityId)
        assertFalse(entity.isRead)
        assertEquals(null, entity.readAt)
        assertTrue(entity.message.orEmpty().contains("Main flat"))
        assertTrue(entity.message.orEmpty().contains("Запланировано"))
        assertTrue(entity.actionPayload.orEmpty().contains("reminderId=${rule.id}"))
        assertTrue(entity.actionPayload.orEmpty().contains("propertyId=${rule.propertyId}"))
    }

    private fun reminderRule(): ReminderRuleEntity =
        ReminderRuleEntity(
            id = "reminder-id",
            userId = "user-id",
            propertyId = "property-id",
            title = "Pay utilities",
            message = "Meter readings and bill",
            type = "UTILITIES",
            scheduleMode = ReminderScheduleMode.ONE_TIME.name,
            oneTimeAt = 2_000L,
            rangeStartAt = null,
            rangeEndAt = null,
            dayOfMonth = null,
            rangeStartDay = null,
            rangeEndDay = null,
            repeatEveryDays = null,
            offsetDays = null,
            hour = 12,
            minute = 0,
            enabled = true,
            nextTriggerAt = 2_000L,
            lastFiredAt = null,
            createdAt = 1_000L,
            updatedAt = 1_000L
        )
}
