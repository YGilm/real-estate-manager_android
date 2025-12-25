package com.example.real_estate_manager.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("user_session")

data class SessionState(
    val userId: String?,
    val rememberMe: Boolean,
    val locked: Boolean,
    val expiresAtMillis: Long,
    val ttlMinutes: Int
) {
    val isSignedIn: Boolean get() = !userId.isNullOrBlank()
}

@Singleton
class UserSession @Inject constructor(
    @ApplicationContext private val context: Context
) {
    internal var nowProvider: () -> Long = { System.currentTimeMillis() }

    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_REMEMBER = booleanPreferencesKey("remember_me")
    private val KEY_LOCKED = booleanPreferencesKey("locked")
    private val KEY_EXPIRES_AT = longPreferencesKey("expires_at")
    private val KEY_TTL_MINUTES = intPreferencesKey("ttl_minutes")

    // ✅ фиксированный TTL: 15 минут (без UI выбора)
    private val FIXED_TTL_MIN = 15

    val stateFlow: Flow<SessionState> = context.dataStore.data.map { prefs ->
        val ttl = FIXED_TTL_MIN
        SessionState(
            userId = prefs[KEY_USER_ID],
            rememberMe = prefs[KEY_REMEMBER] ?: false,
            locked = prefs[KEY_LOCKED] ?: false,
            expiresAtMillis = prefs[KEY_EXPIRES_AT] ?: 0L,
            ttlMinutes = ttl
        )
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }

    // оставляем для совместимости (в UI можно больше не показывать)
    val ttlMinutesFlow: Flow<Int> = context.dataStore.data.map { FIXED_TTL_MIN }

    private fun ttlMs(): Long = FIXED_TTL_MIN.toLong() * 60_000L

    /**
     * Оставлено для совместимости. Теперь TTL фиксированный (15 мин),
     * поэтому метод просто сохраняет 15, чтобы старые вызовы не ломали сборку.
     */
    suspend fun setTtlMinutes(@Suppress("UNUSED_PARAMETER") minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TTL_MINUTES] = FIXED_TTL_MIN
        }
    }

    suspend fun signIn(userId: String, remember: Boolean) {
        val now = nowProvider()
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_REMEMBER] = remember
            prefs[KEY_LOCKED] = false
            prefs[KEY_EXPIRES_AT] = now + ttlMs()
            prefs[KEY_TTL_MINUTES] = FIXED_TTL_MIN
        }
    }

    suspend fun signOut() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_REMEMBER)
            prefs.remove(KEY_LOCKED)
            prefs.remove(KEY_EXPIRES_AT)
            prefs.remove(KEY_TTL_MINUTES)
        }
    }

    /**
     * Вызывать при уходе приложения в фон (ON_STOP).
     * Мы фиксируем "окно" активности на 15 минут.
     */
    suspend fun onAppBackground() {
        val now = nowProvider()
        context.dataStore.edit { prefs ->
            val uid = prefs[KEY_USER_ID]
            if (uid.isNullOrBlank()) return@edit
            // пока юзер активен — продляем окно до now + TTL
            prefs[KEY_EXPIRES_AT] = now + ttlMs()
        }
    }

    /**
     * Вызывать при возвращении в приложение (ON_START).
     * Если TTL истёк:
     *  - remember=false -> вылогин
     *  - remember=true  -> lock (пользователь сохранён, но нужно разблокировать)
     */
    suspend fun onAppForeground() {
        val now = nowProvider()
        context.dataStore.edit { prefs ->
            val uid = prefs[KEY_USER_ID]
            if (uid.isNullOrBlank()) return@edit

            val remember = prefs[KEY_REMEMBER] ?: false
            val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L

            val expired = expiresAt > 0L && now > expiresAt

            if (expired) {
                if (remember) {
                    prefs[KEY_LOCKED] = true
                } else {
                    prefs.remove(KEY_USER_ID)
                    prefs.remove(KEY_REMEMBER)
                    prefs.remove(KEY_LOCKED)
                    prefs.remove(KEY_EXPIRES_AT)
                    prefs.remove(KEY_TTL_MINUTES)
                }
            } else {
                // активная сессия — просто продляем
                prefs[KEY_LOCKED] = false
                prefs[KEY_EXPIRES_AT] = now + ttlMs()
            }
        }
    }

    /**
     * Вызывать после успешной разблокировки (пароль/биометрия).
     * Снимаем lock и продляем TTL.
     */
    suspend fun unlockAndExtend() {
        val now = nowProvider()
        context.dataStore.edit { prefs ->
            val uid = prefs[KEY_USER_ID]
            if (uid.isNullOrBlank()) return@edit
            prefs[KEY_LOCKED] = false
            prefs[KEY_EXPIRES_AT] = now + ttlMs()
            prefs[KEY_TTL_MINUTES] = FIXED_TTL_MIN
        }
    }
}
