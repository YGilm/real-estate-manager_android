package com.example.real_estate_manager.auth

import android.util.Base64
import com.example.real_estate_manager.data.db.UserDao
import com.example.real_estate_manager.data.db.UserEntity
import com.example.real_estate_manager.network.api.AuthApi
import com.example.real_estate_manager.network.dto.LoginRequestDto
import com.example.real_estate_manager.network.dto.RegisterRequestDto
import com.example.real_estate_manager.network.storage.AuthTokenStorage
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val session: UserSession,
    @Named("noAuthAuthApi") private val authApi: AuthApi,
    private val tokenStorage: AuthTokenStorage
) {
    suspend fun getEmailByUserId(userId: String): String? =
        withContext(Dispatchers.IO) {
            tokenStorage.current().email ?: userDao.getById(userId)?.email
        }

    suspend fun register(email: String, password: String, remember: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            validateCredentials(cleanEmail, password)?.let {
                return@withContext Result.failure(IllegalArgumentException(it))
            }

            runCatching {
                authApi.register(
                    RegisterRequestDto(
                        email = cleanEmail,
                        password = password,
                        passwordConfirm = password
                    )
                )
                loginAndPersistSession(cleanEmail, password, remember)
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(IllegalArgumentException(it.toAuthMessage())) }
            )
        }

    suspend fun login(email: String, password: String, remember: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val cleanEmail = email.trim()
            validateCredentials(cleanEmail, password, allowShortPassword = true)?.let {
                return@withContext Result.failure(IllegalArgumentException(it))
            }

            runCatching {
                loginAndPersistSession(cleanEmail, password, remember)
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(IllegalArgumentException(it.toAuthMessage())) }
            )
        }

    suspend fun logout() {
        tokenStorage.clear()
        session.signOut()
    }

    suspend fun restoreSessionFromToken(remember: Boolean = true) {
        val tokens = tokenStorage.current()
        val userId = tokens.userId
        if (tokens.isAuthenticated && !userId.isNullOrBlank()) {
            session.signIn(userId, remember)
        }
    }

    /**
     * Re-auth remains backend-based. Room users are no longer an auth source.
     */
    suspend fun reauth(userId: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Введите пароль"))
            }
            val email = tokenStorage.current().email ?: userDao.getById(userId)?.email
                ?: return@withContext Result.failure(IllegalStateException("Сессия недоступна, войдите заново"))

            runCatching {
                authApi.login(LoginRequestDto(email = email, password = password))
                session.unlockAndExtend()
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { Result.failure(IllegalArgumentException(it.toAuthMessage())) }
            )
        }

    suspend fun updateEmail(userId: String, newEmail: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = newEmail.trim()
            if (clean.isBlank()) {
                Result.failure(IllegalArgumentException("Email не может быть пустым"))
            } else {
                Result.failure(UnsupportedOperationException("Email меняется через backend-профиль"))
            }
        }

    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            when {
                oldPassword.isBlank() -> Result.failure(IllegalArgumentException("Введите текущий пароль"))
                newPassword.isBlank() -> Result.failure(IllegalArgumentException("Введите новый пароль"))
                newPassword.length < 6 -> Result.failure(IllegalArgumentException("Пароль слишком короткий"))
                else -> Result.failure(UnsupportedOperationException("Пароль меняется через backend-профиль"))
            }
        }

    private suspend fun loginAndPersistSession(email: String, password: String, remember: Boolean) {
        val tokens = authApi.login(LoginRequestDto(email = email, password = password))
        val userId = tokens.userId
            ?: decodeUserId(tokens.access)
            ?: email

        tokenStorage.save(
            access = tokens.access,
            refresh = tokens.refresh,
            userId = userId,
            email = tokens.email ?: email
        )

        // Local users table is only a UI/session cache now, never an auth source.
        userDao.insert(
            UserEntity(
                id = userId,
                email = tokens.email ?: email,
                passwordHash = ""
            )
        )
        session.signIn(userId, remember)
    }

    private fun validateCredentials(email: String, password: String, allowShortPassword: Boolean = false): String? =
        when {
            email.isBlank() -> "Введите email"
            !email.contains("@") -> "Введите корректный email"
            password.isBlank() -> "Введите пароль"
            !allowShortPassword && password.length < 6 -> "Пароль слишком короткий"
            else -> null
        }

    private fun decodeUserId(accessToken: String): String? =
        runCatching {
            val payload = accessToken.split(".").getOrNull(1) ?: return null
            val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            val obj = JSONObject(json)
            obj.optString("user_id")
                .ifBlank { obj.optString("sub") }
                .ifBlank { null }
        }.getOrNull()

    private fun Throwable.toAuthMessage(): String =
        when (this) {
            is SocketTimeoutException -> "Сервер недоступен"
            is UnknownHostException -> "Неверный адрес сервера"
            is ConnectException -> "Сервер не запущен"
            is HttpException -> when (code()) {
                400 -> parseBackendError() ?: "Проверьте email и пароль."
                401 -> "Требуется повторный вход"
                403 -> "Доступ запрещен"
                409 -> "Пользователь с таким email уже существует."
                in 500..599 -> "Ошибка сервера"
                else -> parseBackendError() ?: "Ошибка авторизации: ${code()}"
            }
            is IOException -> "Сервер недоступен"
            else -> message ?: "Не удалось выполнить авторизацию."
        }

    private fun HttpException.parseBackendError(): String? =
        runCatching {
            val body = response()?.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
            val json = JSONObject(body)
            listOf("detail", "email", "password", "password_confirm", "non_field_errors")
                .firstNotNullOfOrNull { key ->
                    if (!json.has(key)) null else json.get(key).toReadableError()
                }
        }.getOrNull()

    private fun Any.toReadableError(): String? =
        when (this) {
            is JSONArray -> optString(0)
            else -> toString()
        }.takeIf { it.isNotBlank() }
}
