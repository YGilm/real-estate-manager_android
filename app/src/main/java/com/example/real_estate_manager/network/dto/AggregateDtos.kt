package com.example.real_estate_manager.network.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class PropertyPhotoDto(
    val id: String?,
    @SerializedName(value = "propertyId", alternate = ["property_id", "property"]) val propertyId: String?,
    @SerializedName(value = "imageRef", alternate = ["image_ref"]) val imageRef: String?,
    @SerializedName(value = "photoType", alternate = ["photo_type"]) val photoType: String?,
    @SerializedName(value = "sortOrder", alternate = ["sort_order"]) val sortOrder: Int?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class PropertyDocumentDto(
    val id: String?,
    @SerializedName(value = "propertyId", alternate = ["property_id", "property"]) val propertyId: String?,
    val title: String?,
    @SerializedName(value = "fileRef", alternate = ["file_ref", "file"]) val fileRef: String?,
    @SerializedName(value = "mimeType", alternate = ["mime_type"]) val mimeType: String?,
    @SerializedName(value = "documentType", alternate = ["document_type"]) val documentType: String?,
    @SerializedName(value = "uploadedAt", alternate = ["uploaded_at"]) val uploadedAt: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class UtilityProviderDto(
    val id: String?,
    @SerializedName(value = "propertyId", alternate = ["property_id", "property"]) val propertyId: String?,
    val title: String?,
    @SerializedName(value = "providerType", alternate = ["provider_type"]) val providerType: String?,
    @SerializedName(value = "mosenergoMode", alternate = ["mosenergo_mode"]) val mosenergoMode: String?,
    val configuration: JsonElement?,
    val active: Boolean?,
    val schema: JsonElement?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class CustomProviderFieldDto(
    val id: String?,
    @SerializedName(value = "providerId", alternate = ["provider_id", "provider"]) val providerId: String?,
    val key: String?,
    val label: String?,
    @SerializedName(value = "fieldType", alternate = ["field_type"]) val fieldType: String?,
    val unit: String?,
    val required: Boolean?,
    @SerializedName(value = "sortOrder", alternate = ["sort_order"]) val sortOrder: Int?,
    val configuration: JsonElement?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class MeterReadingDto(
    val id: String?,
    @SerializedName(value = "userId", alternate = ["user_id", "user"]) val userId: String?,
    @SerializedName(value = "propertyId", alternate = ["property_id", "property"]) val propertyId: String?,
    @SerializedName(value = "providerId", alternate = ["provider_id", "provider"]) val providerId: String?,
    @SerializedName(value = "readingDate", alternate = ["reading_date"]) val readingDate: String?,
    @SerializedName(value = "periodYear", alternate = ["period_year"]) val periodYear: Int?,
    @SerializedName(value = "periodMonth", alternate = ["period_month"]) val periodMonth: Int?,
    val values: JsonElement?,
    val consumption: JsonElement?,
    val comment: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)
