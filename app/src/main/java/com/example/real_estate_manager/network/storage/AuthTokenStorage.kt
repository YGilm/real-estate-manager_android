package com.example.real_estate_manager.network.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authTokenDataStore by preferencesDataStore("auth_tokens")

data class AuthTokens(
    val accessToken: String?,
    val refreshToken: String?,
    val userId: String?,
    val email: String?,
    val isAuthenticated: Boolean
)

@Singleton
class AuthTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_ACCESS = stringPreferencesKey("access_token")
    private val KEY_REFRESH = stringPreferencesKey("refresh_token")
    private val KEY_USER_ID = stringPreferencesKey("auth_user_id")
    private val KEY_EMAIL = stringPreferencesKey("auth_email")
    private val KEY_AUTH = booleanPreferencesKey("is_authenticated")

    val tokensFlow: Flow<AuthTokens> = context.authTokenDataStore.data.map { prefs ->
        AuthTokens(
            accessToken = prefs[KEY_ACCESS],
            refreshToken = prefs[KEY_REFRESH],
            userId = prefs[KEY_USER_ID],
            email = prefs[KEY_EMAIL],
            isAuthenticated = prefs[KEY_AUTH] ?: false
        )
    }

    suspend fun current(): AuthTokens = tokensFlow.first()

    suspend fun save(access: String, refresh: String?, userId: String, email: String?) {
        context.authTokenDataStore.edit { prefs ->
            prefs[KEY_ACCESS] = access
            if (refresh != null) prefs[KEY_REFRESH] = refresh
            prefs[KEY_USER_ID] = userId
            if (email != null) prefs[KEY_EMAIL] = email
            prefs[KEY_AUTH] = true
        }
    }

    suspend fun updateAccess(access: String) {
        context.authTokenDataStore.edit { prefs ->
            prefs[KEY_ACCESS] = access
            prefs[KEY_AUTH] = true
        }
    }

    suspend fun clear() {
        context.authTokenDataStore.edit { it.clear() }
    }
}
