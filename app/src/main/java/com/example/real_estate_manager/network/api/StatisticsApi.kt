package com.example.real_estate_manager.network.api

import com.example.real_estate_manager.network.dto.StatisticsDto
import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface StatisticsApi {
    @GET("statistics/")
    suspend fun statistics(
        @Query("property_id") propertyId: String? = null,
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): JsonElement
}
