package com.example.real_estate_manager.network.dto

data class HealthDto(
    val status: String,
    val database: String,
    val version: String,
    val server_time: String
)
