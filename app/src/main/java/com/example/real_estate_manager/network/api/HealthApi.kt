package com.example.real_estate_manager.network.api

import com.example.real_estate_manager.network.dto.HealthDto
import retrofit2.http.GET

interface HealthApi {
    @GET("health/")
    suspend fun health(): HealthDto
}
