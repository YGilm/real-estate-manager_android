package com.example.real_estate_manager.network.dto

import com.google.gson.annotations.SerializedName

data class PropertyDto(
    val id: String?,
    val userId: String?,
    val name: String?,
    val address: String?,
    @SerializedName(value = "squareMeters", alternate = ["square_meters"]) val squareMeters: Double?,
    @SerializedName(value = "monthlyRent", alternate = ["monthly_rent", "rent"]) val monthlyRent: Double?,
    @SerializedName(value = "pricePerM2", alternate = ["price_per_m2"]) val pricePerM2: Double?,
    @SerializedName(value = "cover_uri", alternate = ["coverUri"]) val coverUri: String?,
    @SerializedName(value = "lease_from", alternate = ["leaseFrom"]) val leaseFrom: String?,
    @SerializedName(value = "lease_to", alternate = ["leaseTo"]) val leaseTo: String?,
    @SerializedName(value = "description", alternate = ["detailed_information", "details"]) val description: String?,
    @SerializedName(value = "area_sqm", alternate = ["areaSqm", "area"]) val areaSqm: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class PropertyRequestDto(
    val name: String,
    val address: String?,
    val squareMeters: Double? = null,
    val monthlyRent: Double?,
    val coverUri: String?,
    val leaseFrom: String?,
    val leaseTo: String?,
    val description: String? = null,
    @SerializedName("area_sqm") val areaSqm: String? = null
)
