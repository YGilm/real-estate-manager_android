package com.example.real_estate_manager.network.remote

import com.example.real_estate_manager.network.api.StatisticsApi
import com.example.real_estate_manager.network.dto.StatisticsDto
import com.google.gson.Gson
import javax.inject.Inject

class RemoteStatisticsDataSource @Inject constructor(
    private val api: StatisticsApi,
    private val gson: Gson
) {
    suspend fun statistics(propertyId: String? = null, year: Int? = null, month: Int? = null): StatisticsDto =
        gson.fromJson(api.statistics(propertyId, year, month), StatisticsDto::class.java)
}
