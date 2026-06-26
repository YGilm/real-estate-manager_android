package com.example.real_estate_manager.network.auth

import com.example.real_estate_manager.network.api.AuthApi
import com.example.real_estate_manager.network.dto.RefreshTokenRequestDto
import com.example.real_estate_manager.network.storage.AuthTokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named

class JwtAuthenticator @Inject constructor(
    private val storage: AuthTokenStorage,
    @Named("noAuthAuthApi") private val authApi: AuthApi
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null
        val refresh = runBlocking { storage.current().refreshToken } ?: return null
        val newAccess = runCatching {
            runBlocking { authApi.refresh(RefreshTokenRequestDto(refresh)).access }
        }.getOrNull() ?: return null
        runBlocking { storage.updateAccess(newAccess) }
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
