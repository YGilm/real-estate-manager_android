package com.example.real_estate_manager.network.dto

import com.google.gson.annotations.SerializedName

data class StatisticsTotalsDto(
    val income: Double,
    val expense: Double,
    val total: Double
)

data class StatisticsMonthDto(
    val year: Int,
    val month: Int,
    val income: Double,
    val expense: Double,
    val total: Double
)

data class StatisticsDto(
    val totals: StatisticsTotalsDto? = null,
    @SerializedName(value = "months", alternate = ["monthly_totals", "by_month"]) val months: List<StatisticsMonthDto> = emptyList()
)
