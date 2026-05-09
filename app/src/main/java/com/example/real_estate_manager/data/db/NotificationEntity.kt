package com.example.real_estate_manager.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["userId", "isActive", "createdAt"]),
        Index(value = ["userId", "propertyId"])
    ]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String?,
    val ruleId: String?,
    val title: String,
    val message: String?,
    val createdAt: Long,
    val isActive: Boolean,
    val deactivatedAt: Long?
)
