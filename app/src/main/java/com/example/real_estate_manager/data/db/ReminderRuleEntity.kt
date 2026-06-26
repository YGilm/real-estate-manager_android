package com.example.real_estate_manager.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["userId", "nextTriggerAt"]),
        Index(value = ["userId", "propertyId"]),
        Index(value = ["userId", "syncStatus"])
    ]
)
data class ReminderRuleEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String?,
    val title: String,
    val message: String?,
    val type: String,
    val scheduleMode: String,
    val oneTimeAt: Long?,
    val rangeStartAt: Long?,
    val rangeEndAt: Long?,
    val dayOfMonth: Int?,
    val rangeStartDay: Int?,
    val rangeEndDay: Int?,
    val repeatEveryDays: Int?,
    val offsetDays: Int?,
    val hour: Int = 14,
    val minute: Int = 0,
    val enabled: Boolean,
    val nextTriggerAt: Long,
    val lastFiredAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED",
    val lastSyncError: String? = null,
    val lastSyncAttemptAt: Long? = null
)
