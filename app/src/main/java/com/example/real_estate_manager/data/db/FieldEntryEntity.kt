package com.example.real_estate_manager.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "field_entries",
    indices = [
        Index(value = ["userId", "fieldId"]),
        Index(value = ["userId", "fieldId", "periodYear", "periodMonth"])
    ]
)
data class FieldEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fieldId: String,
    val periodYear: Int,
    val periodMonth: Int,
    val valueNumber: Double?,
    val valueText: String?,
    val status: String?,
    val createdAt: Long
)
