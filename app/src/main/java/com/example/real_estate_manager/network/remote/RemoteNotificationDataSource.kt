package com.example.real_estate_manager.network.remote

import com.example.real_estate_manager.network.api.NotificationApi
import com.example.real_estate_manager.network.dto.NotificationDto
import com.example.real_estate_manager.network.util.decodeListEnvelope
import com.google.gson.Gson
import javax.inject.Inject

class RemoteNotificationDataSource @Inject constructor(
    private val api: NotificationApi,
    private val gson: Gson
) {
    suspend fun fetchActive(): List<NotificationDto> = gson.decodeListEnvelope(api.listActive())
    suspend fun deactivate(id: String): NotificationDto = api.deactivate(id)
}
