package com.example.real_estate_manager.network.api

import com.example.real_estate_manager.network.dto.CustomProviderFieldDto
import com.example.real_estate_manager.network.dto.MeterReadingDto
import com.example.real_estate_manager.network.dto.PropertyDocumentDto
import com.example.real_estate_manager.network.dto.PropertyPhotoDto
import com.example.real_estate_manager.network.dto.UtilityProviderDto
import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PropertyPhotoApi {
    @GET("property-photos/")
    suspend fun list(
        @Query("property_id") propertyId: String? = null,
        @Query("photo_type") photoType: String? = null
    ): Response<JsonElement>

    @POST("property-photos/")
    suspend fun create(@Body body: PropertyPhotoDto): PropertyPhotoDto

    @PATCH("property-photos/{id}/")
    suspend fun patch(@Path("id") id: String, @Body body: PropertyPhotoDto): PropertyPhotoDto

    @DELETE("property-photos/{id}/")
    suspend fun delete(@Path("id") id: String)
}

interface PropertyDocumentApi {
    @GET("property-documents/")
    suspend fun list(
        @Query("property_id") propertyId: String? = null,
        @Query("document_type") documentType: String? = null
    ): Response<JsonElement>

    @POST("property-documents/")
    suspend fun create(@Body body: PropertyDocumentDto): PropertyDocumentDto

    @PATCH("property-documents/{id}/")
    suspend fun patch(@Path("id") id: String, @Body body: PropertyDocumentDto): PropertyDocumentDto

    @DELETE("property-documents/{id}/")
    suspend fun delete(@Path("id") id: String)
}

interface UtilityProviderApi {
    @GET("utility-providers/")
    suspend fun list(
        @Query("property_id") propertyId: String? = null,
        @Query("provider_type") providerType: String? = null,
        @Query("active") active: Boolean? = null
    ): Response<JsonElement>

    @POST("utility-providers/")
    suspend fun create(@Body body: UtilityProviderDto): UtilityProviderDto

    @PATCH("utility-providers/{id}/")
    suspend fun patch(@Path("id") id: String, @Body body: UtilityProviderDto): UtilityProviderDto

    @DELETE("utility-providers/{id}/")
    suspend fun delete(@Path("id") id: String)
}

interface CustomProviderFieldApi {
    @GET("custom-provider-fields/")
    suspend fun list(@Query("provider_id") providerId: String? = null): Response<JsonElement>

    @POST("custom-provider-fields/")
    suspend fun create(@Body body: CustomProviderFieldDto): CustomProviderFieldDto

    @PATCH("custom-provider-fields/{id}/")
    suspend fun patch(@Path("id") id: String, @Body body: CustomProviderFieldDto): CustomProviderFieldDto

    @DELETE("custom-provider-fields/{id}/")
    suspend fun delete(@Path("id") id: String)
}

interface MeterReadingApi {
    @GET("meter-readings/")
    suspend fun list(
        @Query("property_id") propertyId: String? = null,
        @Query("provider_id") providerId: String? = null,
        @Query("period_year") periodYear: Int? = null,
        @Query("period_month") periodMonth: Int? = null
    ): Response<JsonElement>

    @POST("meter-readings/")
    suspend fun create(@Body body: MeterReadingDto): MeterReadingDto

    @PATCH("meter-readings/{id}/")
    suspend fun patch(@Path("id") id: String, @Body body: MeterReadingDto): MeterReadingDto

    @DELETE("meter-readings/{id}/")
    suspend fun delete(@Path("id") id: String)
}
