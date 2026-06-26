package com.example.real_estate_manager.network.api

import com.example.real_estate_manager.network.dto.AccessTokenDto
import com.example.real_estate_manager.network.dto.LoginRequestDto
import com.example.real_estate_manager.network.dto.RefreshTokenRequestDto
import com.example.real_estate_manager.network.dto.RegisterRequestDto
import com.example.real_estate_manager.network.dto.TokenPairDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register/")
    suspend fun register(@Body body: RegisterRequestDto)

    @POST("auth/token/")
    suspend fun login(@Body body: LoginRequestDto): TokenPairDto

    @POST("auth/token/refresh/")
    suspend fun refresh(@Body body: RefreshTokenRequestDto): AccessTokenDto
}
