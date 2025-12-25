package com.example.real_estate_manager.data.model

import java.util.UUID

enum class WidgetFieldType {
    METER,
    MONEY,
    TEXT,
    STATUS,
    IMAGE
}

data class WidgetField(
    val id: String = UUID.randomUUID().toString(),
    val widgetId: String,
    val name: String,
    val fieldType: WidgetFieldType,
    val unit: String? = null,
    val sortOrder: Int = 0
)
