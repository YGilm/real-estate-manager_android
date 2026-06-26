package com.example.real_estate_manager.network.api

import com.example.real_estate_manager.network.dto.TransactionDto
import com.example.real_estate_manager.network.dto.TransactionRequestDto
import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApi {
    @GET("transactions/")
    suspend fun list(@Query("property_id") propertyId: String? = null): JsonElement

    @POST("transactions/")
    suspend fun create(@Body body: TransactionRequestDto): TransactionDto

    @PATCH("transactions/{id}/")
    suspend fun update(@Path("id") id: String, @Body body: TransactionRequestDto): TransactionDto

    @DELETE("transactions/{id}/")
    suspend fun delete(@Path("id") id: String)
}
