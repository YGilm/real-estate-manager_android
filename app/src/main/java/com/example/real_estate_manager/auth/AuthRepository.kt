package com.example.real_estate_manager.auth

import com.example.real_estate_manager.data.db.UserDao
import com.example.real_estate_manager.data.db.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val session: UserSession
) {
    private fun hash(pwd: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pwd.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun matchesPassword(storedHash: String, input: String): Boolean =
        storedHash == hash(input) || storedHash == input

    private suspend fun upgradeLegacyPasswordHash(user: UserEntity, input: String) {
        val newHash = hash(input)
        if (user.passwordHash == input && user.passwordHash != newHash) {
            userDao.updatePasswordHash(user.id, newHash)
        }
    }

    suspend fun getEmailByUserId(userId: String): String? =
        withContext(Dispatchers.IO) { userDao.getById(userId)?.email }

    suspend fun register(email: String, password: String, remember: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val e = email.trim()
            if (e.isBlank()) return@withContext Result.failure(IllegalArgumentException("Введите email"))
            if (password.isBlank()) return@withContext Result.failure(IllegalArgumentException("Введите пароль"))

            val exists = userDao.getByEmail(e)
            if (exists != null) {
                return@withContext Result.failure(IllegalStateException("Пользователь уже существует"))
            }

            val id = UUID.randomUUID().toString()
            userDao.insert(UserEntity(id = id, email = e, passwordHash = hash(password)))
            session.signIn(id, remember)
            Result.success(Unit)
        }

    suspend fun login(email: String, password: String, remember: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val e = email.trim()
            val user = userDao.getByEmail(e)
                ?: return@withContext Result.failure(IllegalArgumentException("Неверный email/пароль"))

            if (!matchesPassword(user.passwordHash, password)) {
                return@withContext Result.failure(IllegalArgumentException("Неверный email/пароль"))
            }

            upgradeLegacyPasswordHash(user, password)
            session.signIn(user.id, remember)
            Result.success(Unit)
        }

    suspend fun logout() = session.signOut()

    /**
     * Разблокировка remembered-сессии по паролю (когда session.locked == true).
     */
    suspend fun reauth(userId: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (password.isBlank()) return@withContext Result.failure(IllegalArgumentException("Введите пароль"))

            val user = userDao.getById(userId)
                ?: return@withContext Result.failure(IllegalStateException("Пользователь не найден"))

            if (!matchesPassword(user.passwordHash, password)) {
                return@withContext Result.failure(IllegalArgumentException("Неверный пароль"))
            }

            upgradeLegacyPasswordHash(user, password)
            session.unlockAndExtend()
            Result.success(Unit)
        }

    suspend fun updateEmail(userId: String, newEmail: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val clean = newEmail.trim()
            if (clean.isBlank()) return@withContext Result.failure(IllegalArgumentException("Email не может быть пустым"))

            val exists = userDao.getByEmail(clean)
            if (exists != null && exists.id != userId) {
                return@withContext Result.failure(IllegalStateException("Этот email уже занят"))
            }

            userDao.updateEmail(userId, clean)
            Result.success(Unit)
        }

    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (oldPassword.isBlank()) return@withContext Result.failure(IllegalArgumentException("Введите текущий пароль"))
            if (newPassword.isBlank()) return@withContext Result.failure(IllegalArgumentException("Введите новый пароль"))
            if (newPassword.length < 4) return@withContext Result.failure(IllegalArgumentException("Пароль слишком короткий"))

            val user = userDao.getById(userId)
                ?: return@withContext Result.failure(IllegalStateException("Пользователь не найден"))

            if (!matchesPassword(user.passwordHash, oldPassword)) {
                return@withContext Result.failure(IllegalArgumentException("Текущий пароль неверный"))
            }

            userDao.updatePasswordHash(userId, hash(newPassword))
            Result.success(Unit)
        }
}
