package com.example.real_estate_manager.data.model

import java.util.UUID

data class Property(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String? = null,
    val squareMeters: Double? = null,
    val monthlyRent: Double? = null,
    val pricePerM2: Double? = null,
    val coverUri: String? = null,
    val leaseFrom: String? = null,
    val leaseTo: String? = null
)
