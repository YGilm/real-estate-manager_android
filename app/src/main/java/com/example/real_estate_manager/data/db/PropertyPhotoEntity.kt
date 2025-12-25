package com.example.real_estate_manager.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "property_photos",
    indices = [
        Index(value = ["userId", "propertyId"])
    ]
)
data class PropertyPhotoEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String,
    /** content://... (через SAF + takePersistableUriPermission) */
    val uri: String,
    val createdAt: Long
)