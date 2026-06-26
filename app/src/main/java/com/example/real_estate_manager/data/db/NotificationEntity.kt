package com.example.real_estate_manager.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["userId", "isRead", "createdAt"]),
        Index(value = ["userId", "relatedEntityType", "relatedEntityId"]),
        Index(value = ["userId", "type", "createdAt"])
    ]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String?,
    val type: String,
    val relatedEntityId: String?,
    val relatedEntityType: String?,
    val isRead: Boolean,
    val createdAt: Long,
    val readAt: Long?,
    val actionPayload: String?,
    val syncStatus: String = "SYNCED",
    val lastSyncError: String? = null,
    val updatedAt: Long = createdAt
) {
    val propertyId: String?
        get() = if (relatedEntityType == NotificationRelatedEntityType.PROPERTY.name) relatedEntityId else null

    val ruleId: String?
        get() = if (relatedEntityType == NotificationRelatedEntityType.REMINDER.name) relatedEntityId else null
}

enum class NotificationType {
    REMINDER,
    SYSTEM,
    PROPERTY,
    PAYMENT,
    SYNC_ERROR
}

enum class NotificationRelatedEntityType {
    REMINDER,
    PROPERTY,
    PAYMENT
}
