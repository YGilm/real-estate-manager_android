package com.example.real_estate_manager.data.db

import androidx.room.Entity

@Entity(
    tableName = "property_details",
    primaryKeys = ["userId", "propertyId"]
)
data class PropertyDetailsEntity(
    val userId: String,
    val propertyId: String,
    val description: String?,
    /** Храним строкой, например "45.50" (без локализации), чтобы не ловить float-баги */
    val areaSqm: String?,
    val updatedAt: Long
)