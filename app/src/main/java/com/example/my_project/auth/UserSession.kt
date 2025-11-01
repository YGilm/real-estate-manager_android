package com.example.my_project.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("user_session")

@Singleton
class UserSession @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_REMEMBER = booleanPreferencesKey("remember_me")

    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val rememberFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_REMEMBER] ?: false }

    suspend fun signIn(userId: String, remember: Boolean) {
        context.dataStore.edit {
            it[KEY_USER_ID] = userId
            it[KEY_REMEMBER] = remember
        }
    }

    suspend fun signOut() {
        context.dataStore.edit {
            it.remove(KEY_USER_ID)
            it.remove(KEY_REMEMBER)
        }
    }
}