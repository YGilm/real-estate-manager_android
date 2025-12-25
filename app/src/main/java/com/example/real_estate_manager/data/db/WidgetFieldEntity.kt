package com.example.real_estate_manager.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "widget_fields",
    indices = [
        Index(value = ["userId", "widgetId"])
    ]
)
data class WidgetFieldEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val widgetId: String,
    val name: String,
    val fieldType: String,
    val unit: String?,
    val sortOrder: Int
)
