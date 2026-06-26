package com.example.real_estate_manager.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(
    tableName = "property_documents",
    indices = [
        Index(value = ["userId", "propertyId"]),
        Index(value = ["userId", "propertyId", "documentType"])
    ]
)
data class PropertyDocumentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String,
    val title: String,
    val fileRef: String,
    val mimeType: String,
    val documentType: String?,
    val uploadedAt: String?,
    val createdAt: String?,
    val updatedAt: String?
)

@Entity(
    tableName = "utility_providers",
    indices = [
        Index(value = ["userId", "propertyId"]),
        Index(value = ["userId", "propertyId", "active"]),
        Index(value = ["userId", "propertyId", "providerType"]),
        Index(value = ["userId", "syncStatus"])
    ]
)
data class UtilityProviderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String,
    val title: String,
    val providerType: String,
    val mosenergoMode: String?,
    val configurationJson: String,
    val active: Boolean,
    val schemaJson: String,
    val createdAt: String?,
    val updatedAt: String?,
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED",
    val lastSyncError: String? = null,
    val lastSyncAttemptAt: Long? = null
)

@Entity(
    tableName = "custom_provider_fields",
    indices = [
        Index(value = ["userId", "providerId"]),
        Index(value = ["userId", "providerId", "sortOrder"]),
        Index(value = ["userId", "syncStatus"])
    ]
)
data class CustomProviderFieldEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val providerId: String,
    val key: String,
    val label: String,
    val fieldType: String,
    val unit: String?,
    val required: Boolean,
    val sortOrder: Int,
    val configurationJson: String,
    val createdAt: String?,
    val updatedAt: String?,
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED",
    val lastSyncError: String? = null,
    val lastSyncAttemptAt: Long? = null
)

@Entity(
    tableName = "meter_readings",
    indices = [
        Index(value = ["userId", "propertyId"]),
        Index(value = ["userId", "providerId"]),
        Index(value = ["userId", "syncStatus"]),
        Index(value = ["userId", "propertyId", "periodYear", "periodMonth"]),
        Index(value = ["userId", "providerId", "periodYear", "periodMonth"], unique = true)
    ]
)
data class MeterReadingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String,
    val providerId: String,
    val readingDate: String,
    val periodYear: Int,
    val periodMonth: Int,
    val valuesJson: String,
    val consumptionJson: String,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String?,
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED",
    val lastSyncError: String? = null,
    val lastSyncAttemptAt: Long? = null
)
