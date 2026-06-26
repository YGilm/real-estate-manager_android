package com.example.real_estate_manager.network.dto

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    val id: String,
    @SerializedName("property_id") val propertyId: String?,
    @SerializedName("rule_id") val ruleId: String?,
    val title: String,
    val message: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("deactivated_at") val deactivatedAt: Long?
)
