package com.example.real_estate_manager.data

import android.util.Log
import com.example.real_estate_manager.network.dto.StatisticsDto
import com.example.real_estate_manager.network.remote.RemoteStatisticsDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudStatisticsRepository @Inject constructor(
    private val remote: RemoteStatisticsDataSource
) : StatisticsRepository {
    override suspend fun statistics(propertyId: String?, year: Int?, month: Int?): Result<StatisticsDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                remote.statistics(propertyId, year, month)
            }.onSuccess {
                Log.d("CloudSync", "statistics fetched propertyId=$propertyId year=$year month=$month months=${it.months.size}")
            }.onFailure {
                Log.w("CloudSync", "statistics fetch failed: ${it.message}")
            }
        }
}
