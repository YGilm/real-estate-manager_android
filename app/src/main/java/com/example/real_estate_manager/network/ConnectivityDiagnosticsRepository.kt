package com.example.real_estate_manager.network

import android.util.Base64
import android.util.Log
import com.example.real_estate_manager.network.api.HealthApi
import com.example.real_estate_manager.network.dto.HealthDto
import com.example.real_estate_manager.network.storage.AuthTokenStorage
import com.example.real_estate_manager.network.storage.ServerConfigStore
import com.example.real_estate_manager.network.util.toUserMessage
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class HealthCheckResult(
    val ok: Boolean,
    val message: String,
    val health: HealthDto? = null
)

data class DiagnosticsSnapshot(
    val serverUrl: String,
    val currentUser: String?,
    val tokenExpiresAt: String?,
    val lastSuccessfulSyncMillis: Long?,
    val lastError: String?
)

@Singleton
class ConnectivityDiagnosticsRepository @Inject constructor(
    private val healthApi: HealthApi,
    private val serverConfigStore: ServerConfigStore,
    private val serverDiscoveryRepository: ServerDiscoveryRepository,
    private val authTokenStorage: AuthTokenStorage
) {
    companion object {
        private const val TAG = "ServerDiagnostics"
    }

    val configFlow = serverConfigStore.configFlow

    suspend fun ensureDefaultServerUrl() = serverConfigStore.ensureDefaultServerUrl()

    suspend fun saveServerUrl(url: String) = serverConfigStore.saveServerUrl(url)

    suspend fun checkConnection(): HealthCheckResult = withContext(Dispatchers.IO) {
        val currentConfig = serverConfigStore.current()
        val discovery = serverDiscoveryRepository.discover(currentConfig.serverUrl)
        if (!discovery.found) {
            serverConfigStore.markError(discovery.message)
            return@withContext HealthCheckResult(ok = false, message = discovery.message)
        }

        val foundUrl = discovery.foundUrl.orEmpty()
        val autoDiscovered = foundUrl != currentConfig.serverUrl
        if (autoDiscovered) {
            serverConfigStore.saveServerUrl(foundUrl)
        }

        runCatching { healthApi.health() }.fold(
            onSuccess = { health ->
                serverConfigStore.markSuccess()
                HealthCheckResult(
                    ok = true,
                    message = if (autoDiscovered) discovery.message else "Сервер найден",
                    health = health
                )
            },
            onFailure = { error ->
                val message = if (autoDiscovered) {
                    "Сервер найден, но health-check вернул ошибку"
                } else {
                    error.toDiagnosticsMessage(currentConfig.serverUrl)
                }
                Log.e(TAG, "Health check failed: ${error::class.java.simpleName}: ${error.message}", error)
                serverConfigStore.markError("$message: ${error.toUserMessage()}")
                HealthCheckResult(ok = false, message = message)
            }
        )
    }

    suspend fun diagnosticsSnapshot(): DiagnosticsSnapshot = withContext(Dispatchers.IO) {
        val config = serverConfigStore.current()
        val tokens = authTokenStorage.current()
        DiagnosticsSnapshot(
            serverUrl = config.serverUrl,
            currentUser = tokens.email ?: tokens.userId,
            tokenExpiresAt = tokenExpiration(tokens.accessToken),
            lastSuccessfulSyncMillis = config.lastSuccessfulSyncMillis,
            lastError = config.lastError
        )
    }

    private fun tokenExpiration(token: String?): String? =
        runCatching {
            val payload = token?.split(".")?.getOrNull(1) ?: return null
            val json = String(
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                StandardCharsets.UTF_8
            )
            val exp = JSONObject(json).optLong("exp", 0L)
            if (exp == 0L) null else java.time.Instant.ofEpochSecond(exp).toString()
        }.getOrNull()

    private fun Throwable.toDiagnosticsMessage(serverUrl: String): String =
        when (this) {
            is SocketTimeoutException -> "Порт 8000 недоступен: $serverUrl"
            is UnknownHostException -> "Не удалось подключиться к $serverUrl"
            is ConnectException -> "Не удалось подключиться к $serverUrl"
            is HttpException -> when (code()) {
                401, 403 -> "Ошибка авторизации"
                400 -> "Ошибка конфигурации сервера"
                in 500..599 -> "Ошибка сервера"
                else -> "Ошибка сети"
            }
            is java.io.IOException -> "Не удалось подключиться к $serverUrl"
            else -> "Ошибка сети"
        }
}
