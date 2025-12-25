package com.example.real_estate_manager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "properties")
data class PropertyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val address: String?,
    val monthlyRent: Double?,
    val coverUri: String?,
    val leaseFrom: String?,   // договор c
    val leaseTo: String?      // договор по
)