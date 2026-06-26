package com.example.real_estate_manager.network.dto

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement

data class TransactionDto(
    val id: String?,
    @SerializedName(value = "propertyId", alternate = ["property_id"]) val propertyId: String?,
    val property: JsonElement?,
    val type: String?,
    @SerializedName(value = "is_income", alternate = ["isIncome"]) val isIncome: Boolean?,
    val amount: Double?,
    @SerializedName(value = "dateIso", alternate = ["date", "transaction_date", "created_date"]) val date: String?,
    val note: String?,
    @SerializedName(value = "attachment_uri", alternate = ["attachmentUri"]) val attachmentUri: String?,
    @SerializedName(value = "attachment_name", alternate = ["attachmentName"]) val attachmentName: String?,
    @SerializedName(value = "attachment_mime", alternate = ["attachmentMime"]) val attachmentMime: String?
)

data class TransactionRequestDto(
    @SerializedName(value = "propertyId", alternate = ["property_id"]) val propertyId: String,
    val isIncome: Boolean,
    val amount: Double,
    val dateIso: String,
    val note: String?,
    @SerializedName("attachment_uri") val attachmentUri: String?,
    @SerializedName("attachment_name") val attachmentName: String?,
    @SerializedName("attachment_mime") val attachmentMime: String?
)
