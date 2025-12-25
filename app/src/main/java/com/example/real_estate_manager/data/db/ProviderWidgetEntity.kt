package com.example.real_estate_manager.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "provider_widgets",
    indices = [
        Index(value = ["userId", "propertyId"])
    ]
)
data class ProviderWidgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String,
    val type: String,
    val title: String,
    val templateKey: String,
    val createdAt: Long,
    val archived: Boolean
)
