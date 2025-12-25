package com.example.real_estate_manager.data.model

import java.util.UUID

enum class ProviderWidgetType {
    METER_PROVIDER,
    PAYMENT,
    CUSTOM
}

data class ProviderWidget(
    val id: String = UUID.randomUUID().toString(),
    val propertyId: String,
    val type: ProviderWidgetType,
    val title: String,
    val templateKey: String,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)
