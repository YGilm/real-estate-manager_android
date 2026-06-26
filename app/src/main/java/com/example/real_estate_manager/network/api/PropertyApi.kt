package com.example.real_estate_manager.network.api

import com.example.real_estate_manager.network.dto.PropertyDto
import com.example.real_estate_manager.network.dto.PropertyRequestDto
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response

interface PropertyApi {
    @GET("properties/")
    suspend fun list(): Response<JsonElement>

    @POST("properties/")
    suspend fun create(@Body body: PropertyRequestDto): PropertyDto

    @PATCH("properties/{id}/")
    suspend fun update(@Path("id") id: String, @Body body: PropertyRequestDto): PropertyDto

    @DELETE("properties/{id}/")
    suspend fun delete(@Path("id") id: String)
}
