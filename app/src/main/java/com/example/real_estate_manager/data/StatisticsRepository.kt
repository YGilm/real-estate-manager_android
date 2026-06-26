package com.example.real_estate_manager.data

import com.example.real_estate_manager.network.dto.StatisticsDto

interface StatisticsRepository {
    suspend fun statistics(propertyId: String?, year: Int?, month: Int?): Result<StatisticsDto>
}
