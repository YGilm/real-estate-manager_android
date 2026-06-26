package com.example.real_estate_manager.network.remote

import android.util.Log
import com.example.real_estate_manager.network.api.PropertyApi
import com.example.real_estate_manager.network.dto.PropertyDto
import com.example.real_estate_manager.network.dto.PropertyRequestDto
import com.example.real_estate_manager.network.util.decodeListEnvelope
import com.google.gson.Gson
import com.google.gson.JsonElement
import javax.inject.Inject
import retrofit2.Response

class RemotePropertyDataSource @Inject constructor(
    private val api: PropertyApi,
    private val gson: Gson
) {
    suspend fun fetchAll(): List<PropertyDto> {
        val response = api.list()
        val body = response.requireBody("properties")
        val list = gson.decodeListEnvelope<PropertyDto>(body)
        Log.d(TAG, "GET ${response.raw().request.url} code=${response.code()} properties fetched=${list.size}")
        Log.d(TAG, "properties raw=$body")
        list.firstOrNull { it.name.equals(TARGET_PROPERTY, ignoreCase = true) }?.let {
            Log.d(
                TAG,
                "PropertyDto target id=${it.id} name=${it.name} squareMeters=${it.squareMeters} " +
                    "pricePerM2=${it.pricePerM2} monthlyRent=${it.monthlyRent} leaseFrom=${it.leaseFrom} leaseTo=${it.leaseTo}"
            )
        }
        return list
    }

    suspend fun create(body: PropertyRequestDto): PropertyDto = api.create(body)
    suspend fun update(id: String, body: PropertyRequestDto): PropertyDto = api.update(id, body)
    suspend fun delete(id: String) = api.delete(id)

    private companion object {
        const val TAG = "PropertySync"
        const val TARGET_PROPERTY = "Скандинавия центр"
    }
}

private fun Response<JsonElement>.requireBody(label: String): JsonElement {
    if (!isSuccessful) {
        error("$label request failed code=${code()} url=${raw().request.url} error=${errorBody()?.string()}")
    }
    return body() ?: error("$label request returned empty body code=${code()} url=${raw().request.url}")
}
