package com.example.real_estate_manager.reminders

import com.example.real_estate_manager.data.db.NotificationEntity
import com.example.real_estate_manager.data.db.NotificationRelatedEntityType
import com.example.real_estate_manager.data.db.NotificationType
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderNotificationFactory {
    fun createEntity(
        id: String,
        rule: ReminderRuleEntity,
        propertyName: String?,
        createdAt: Long
    ): NotificationEntity {
        val propertyLine = propertyName?.let { "Объект: $it" } ?: "Объект: без привязки"
        val message = rule.message?.takeIf { it.isNotBlank() }
        val scheduledLine = "Запланировано: ${formatTime(rule.nextTriggerAt)}"
        val details = buildString {
            append(propertyLine)
            append("\n")
            append(scheduledLine)
            if (!message.isNullOrBlank()) {
                append("\n")
                append(message)
            }
        }
        return NotificationEntity(
            id = id,
            userId = rule.userId,
            title = rule.title.ifBlank { "Напоминание" },
            message = details,
            type = NotificationType.REMINDER.name,
            relatedEntityId = rule.id,
            relatedEntityType = NotificationRelatedEntityType.REMINDER.name,
            isRead = false,
            createdAt = createdAt,
            readAt = null,
            actionPayload = buildActionPayload(rule),
            updatedAt = createdAt
        )
    }

    fun buildActionPayload(rule: ReminderRuleEntity): String =
        listOfNotNull(
            "reminderId=${rule.id}",
            rule.propertyId?.let { "propertyId=$it" },
            "scheduledAt=${rule.nextTriggerAt}"
        ).joinToString("&")

    fun formatTime(value: Long): String =
        if (value == Long.MAX_VALUE) {
            "не запланировано"
        } else {
            Instant.ofEpochMilli(value)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        }
}
