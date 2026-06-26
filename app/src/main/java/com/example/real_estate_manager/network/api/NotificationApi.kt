package com.example.real_estate_manager.network.api

import com.example.real_estate_manager.network.dto.NotificationDto
import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationApi {
    @GET("notifications/")
    suspend fun listActive(): JsonElement

    @PATCH("notifications/{id}/deactivate/")
    suspend fun deactivate(@Path("id") id: String): NotificationDto
}
