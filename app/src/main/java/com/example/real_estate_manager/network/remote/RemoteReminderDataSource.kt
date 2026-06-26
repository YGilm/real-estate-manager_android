package com.example.real_estate_manager.network.remote

import android.util.Log
import com.example.real_estate_manager.network.api.ReminderApi
import com.example.real_estate_manager.network.dto.ReminderDto
import com.example.real_estate_manager.network.dto.ReminderRequestDto
import com.example.real_estate_manager.network.util.decodeListEnvelope
import com.google.gson.Gson
import com.google.gson.JsonElement
import javax.inject.Inject
import retrofit2.HttpException
import retrofit2.Response

class RemoteReminderDataSource @Inject constructor(
    private val api: ReminderApi,
    private val gson: Gson
) {
    suspend fun fetchAll(propertyId: String? = null): List<ReminderDto> =
        gson.decodeListEnvelope(api.list(propertyId))
    suspend fun create(body: ReminderRequestDto): ReminderDto =
        api.create(body).decodeLogged("POST", "reminders/", body)
    suspend fun update(id: String, body: ReminderRequestDto): ReminderDto =
        api.update(id, body).decodeLogged("PATCH", "reminders/$id/", body)
    suspend fun delete(id: String) = api.delete(id)

    private fun Response<JsonElement>.decodeLogged(
        method: String,
        endpoint: String,
        requestBody: ReminderRequestDto
    ): ReminderDto {
        val requestJson = gson.toJson(requestBody)
        val url = raw().request.url.toString()
        Log.d(TAG, "$method $endpoint url=$url request=$requestJson")
        Log.d(TAG, "$method $endpoint code=${code()}")
        if (!isSuccessful) {
            val errorBody = errorBody()?.string().orEmpty()
            Log.w(TAG, "$method $endpoint url=$url code=${code()} errorBody=$errorBody request=$requestJson")
            throw HttpException(this)
        }
        val responseBody = body() ?: error("$method $endpoint returned empty body code=${code()} url=$url")
        Log.d(TAG, "$method $endpoint response=$responseBody")
        return gson.fromJson(responseBody, ReminderDto::class.java)
    }

    private companion object {
        const val TAG = "ReminderSync"
    }
}
