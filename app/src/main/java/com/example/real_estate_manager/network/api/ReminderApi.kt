package com.example.real_estate_manager.network.api

import com.example.real_estate_manager.network.dto.ReminderDto
import com.example.real_estate_manager.network.dto.ReminderRequestDto
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReminderApi {
    @GET("reminders/")
    suspend fun list(@Query("property_id") propertyId: String? = null): JsonElement

    @POST("reminders/")
    suspend fun create(@Body body: ReminderRequestDto): Response<JsonElement>

    @PATCH("reminders/{id}/")
    suspend fun update(@Path("id") id: String, @Body body: ReminderRequestDto): Response<JsonElement>

    @DELETE("reminders/{id}/")
    suspend fun delete(@Path("id") id: String)
}
