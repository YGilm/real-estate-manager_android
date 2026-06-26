package com.example.real_estate_manager.network.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.real_estate_manager.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverConfigDataStore by preferencesDataStore("server_config")

data class ServerConfig(
    val serverUrl: String,
    val lastSuccessfulSyncMillis: Long?,
    val lastError: String?,
    val userConfigured: Boolean,
    val editable: Boolean
)

@Singleton
class ServerConfigStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_SERVER_PORT = 8000
        const val DEBUG_SERVER_URL = "http://10.0.2.2:8000/"
        const val RELEASE_SERVER_URL = "https://api.real-estate-app.ru/"
    }

    private val defaultServerUrl = if (BuildConfig.DEBUG) DEBUG_SERVER_URL else RELEASE_SERVER_URL

    private val KEY_USER_CONFIGURED = booleanPreferencesKey("user_configured")
    private val KEY_SERVER_URL = stringPreferencesKey("server_url")
    private val KEY_LAST_SUCCESS = longPreferencesKey("last_successful_sync_millis")
    private val KEY_LAST_ERROR = stringPreferencesKey("last_error")

    val configFlow: Flow<ServerConfig> = context.serverConfigDataStore.data.map { prefs ->
        val storedServerUrl = prefs[KEY_SERVER_URL]
        val serverUrl = if (BuildConfig.DEBUG) {
            storedServerUrl ?: defaultServerUrl
        } else {
            RELEASE_SERVER_URL
        }
        ServerConfig(
            serverUrl = normalizeServerUrl(serverUrl),
            lastSuccessfulSyncMillis = prefs[KEY_LAST_SUCCESS],
            lastError = prefs[KEY_LAST_ERROR],
            userConfigured = prefs[KEY_USER_CONFIGURED] ?: false,
            editable = BuildConfig.DEBUG
        )
    }

    suspend fun current(): ServerConfig {
        ensureDefaultServerUrl()
        return configFlow.first()
    }

    suspend fun apiBaseUrl(): String = toApiBaseUrl(current().serverUrl)

    suspend fun ensureDefaultServerUrl() {
        if (!BuildConfig.DEBUG) return
        context.serverConfigDataStore.edit { prefs ->
            if (!prefs.contains(KEY_SERVER_URL)) {
                prefs[KEY_SERVER_URL] = normalizeServerUrl(DEBUG_SERVER_URL)
                prefs[KEY_USER_CONFIGURED] = false
            }
        }
    }

    suspend fun saveServerUrl(url: String) {
        if (!BuildConfig.DEBUG) return
        context.serverConfigDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = normalizeServerUrl(url)
            prefs[KEY_USER_CONFIGURED] = true
        }
    }

    suspend fun markSuccess(timestampMillis: Long = System.currentTimeMillis()) {
        context.serverConfigDataStore.edit { prefs ->
            prefs[KEY_LAST_SUCCESS] = timestampMillis
            prefs.remove(KEY_LAST_ERROR)
        }
    }

    suspend fun markError(message: String) {
        context.serverConfigDataStore.edit { prefs ->
            prefs[KEY_LAST_ERROR] = message
        }
    }

    private fun normalizeServerUrl(raw: String): String {
        val trimmed = raw.trim().ifBlank { defaultServerUrl }
        val withoutApi = trimmed
            .removeSuffix("/")
            .removeSuffix("/api")
            .removeSuffix("/api/")
        return "$withoutApi/"
    }

    private fun toApiBaseUrl(serverUrl: String): String = "${normalizeServerUrl(serverUrl)}api/"
}
