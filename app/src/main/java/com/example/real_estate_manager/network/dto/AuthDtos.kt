package com.example.real_estate_manager.network.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerializedName("password_confirm") val passwordConfirm: String
)

data class RefreshTokenRequestDto(
    val refresh: String
)

data class TokenPairDto(
    val access: String,
    val refresh: String,
    @SerializedName("user_id") val userId: String? = null,
    val email: String? = null
)

data class AccessTokenDto(
    val access: String
)
