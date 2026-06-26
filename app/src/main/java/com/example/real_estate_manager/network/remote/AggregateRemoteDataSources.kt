package com.example.real_estate_manager.network.remote

import android.util.Log
import com.example.real_estate_manager.network.api.CustomProviderFieldApi
import com.example.real_estate_manager.network.api.MeterReadingApi
import com.example.real_estate_manager.network.api.PropertyDocumentApi
import com.example.real_estate_manager.network.api.PropertyPhotoApi
import com.example.real_estate_manager.network.api.UtilityProviderApi
import com.example.real_estate_manager.network.dto.CustomProviderFieldDto
import com.example.real_estate_manager.network.dto.MeterReadingDto
import com.example.real_estate_manager.network.dto.PropertyDocumentDto
import com.example.real_estate_manager.network.dto.PropertyPhotoDto
import com.example.real_estate_manager.network.dto.UtilityProviderDto
import com.example.real_estate_manager.network.util.decodeListEnvelope
import com.google.gson.Gson
import com.google.gson.JsonElement
import javax.inject.Inject
import retrofit2.Response

class RemotePropertyPhotoDataSource @Inject constructor(
    private val api: PropertyPhotoApi,
    private val gson: Gson
) {
    suspend fun fetchAll(propertyId: String? = null, photoType: String? = null): List<PropertyPhotoDto> =
        api.list(propertyId, photoType).decodeLogged(gson, "PropertySync", "property-photos")
    suspend fun create(body: PropertyPhotoDto): PropertyPhotoDto = api.create(body)
    suspend fun patch(id: String, body: PropertyPhotoDto): PropertyPhotoDto = api.patch(id, body)
    suspend fun delete(id: String) = api.delete(id)
}

class RemotePropertyDocumentDataSource @Inject constructor(
    private val api: PropertyDocumentApi,
    private val gson: Gson
) {
    suspend fun fetchAll(propertyId: String? = null, documentType: String? = null): List<PropertyDocumentDto> =
        api.list(propertyId, documentType).decodeLogged(gson, "PropertySync", "property-documents")
    suspend fun create(body: PropertyDocumentDto): PropertyDocumentDto = api.create(body)
    suspend fun patch(id: String, body: PropertyDocumentDto): PropertyDocumentDto = api.patch(id, body)
    suspend fun delete(id: String) = api.delete(id)
}

class RemoteUtilityProviderDataSource @Inject constructor(
    private val api: UtilityProviderApi,
    private val gson: Gson
) {
    suspend fun fetchAll(propertyId: String? = null, providerType: String? = null, active: Boolean? = null): List<UtilityProviderDto> =
        api.list(propertyId, providerType, active).decodeLogged(gson, "UtilityProviderSync", "providers")
    suspend fun create(body: UtilityProviderDto): UtilityProviderDto = api.create(body)
    suspend fun patch(id: String, body: UtilityProviderDto): UtilityProviderDto = api.patch(id, body)
    suspend fun delete(id: String) = api.delete(id)
}

class RemoteCustomProviderFieldDataSource @Inject constructor(
    private val api: CustomProviderFieldApi,
    private val gson: Gson
) {
    suspend fun fetchAll(providerId: String? = null): List<CustomProviderFieldDto> =
        api.list(providerId).decodeLogged(gson, "UtilityProviderSync", "custom-fields")
    suspend fun create(body: CustomProviderFieldDto): CustomProviderFieldDto = api.create(body)
    suspend fun patch(id: String, body: CustomProviderFieldDto): CustomProviderFieldDto = api.patch(id, body)
    suspend fun delete(id: String) = api.delete(id)
}

class RemoteMeterReadingDataSource @Inject constructor(
    private val api: MeterReadingApi,
    private val gson: Gson
) {
    suspend fun fetchAll(
        propertyId: String? = null,
        providerId: String? = null,
        periodYear: Int? = null,
        periodMonth: Int? = null
    ): List<MeterReadingDto> =
        api.list(propertyId, providerId, periodYear, periodMonth).decodeLogged(gson, "MeterReadingSync", "readings")
    suspend fun create(body: MeterReadingDto): MeterReadingDto = api.create(body)
    suspend fun patch(id: String, body: MeterReadingDto): MeterReadingDto = api.patch(id, body)
    suspend fun delete(id: String) = api.delete(id)
}

private inline fun <reified T> Response<JsonElement>.decodeLogged(
    gson: Gson,
    tag: String,
    label: String
): List<T> {
    if (!isSuccessful) {
        error("$label request failed code=${code()} url=${raw().request.url} error=${errorBody()?.string()}")
    }
    val body = body() ?: error("$label request returned empty body code=${code()} url=${raw().request.url}")
    val list = gson.decodeListEnvelope<T>(body)
    Log.d(tag, "GET ${raw().request.url} code=${code()} $label fetched=${list.size}")
    Log.d(tag, "$label raw=$body")
    return list
}
