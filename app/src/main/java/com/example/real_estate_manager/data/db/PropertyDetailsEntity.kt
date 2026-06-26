package com.example.real_estate_manager.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "property_details",
    primaryKeys = ["userId", "propertyId"],
    indices = [Index(value = ["userId", "syncStatus"])]
)
data class PropertyDetailsEntity(
    val userId: String,
    val propertyId: String,
    val description: String?,
    /** Храним строкой, например "45.50" (без локализации), чтобы не ловить float-баги */
    val areaSqm: String?,
    val updatedAt: Long,
    /** SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE. */
    @ColumnInfo(defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED",
    val lastSyncError: String? = null,
    val lastSyncAttemptAt: Long? = null
)
