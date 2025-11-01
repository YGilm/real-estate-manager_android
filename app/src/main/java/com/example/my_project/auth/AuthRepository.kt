package com.example.my_project.auth

import com.example.my_project.data.db.UserDao
import com.example.my_project.data.db.UserEntity
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
        MessageDigest.getInstance("SHA-256").digest(pwd.toByteArray())
            .joinToString("") { "%02x".format(it) }

    suspend fun register(email: String, password: String, remember: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val exists = userDao.getByEmail(email)
            if (exists != null) return@withContext Result.failure(IllegalStateException("Пользователь уже существует"))
            val id = UUID.randomUUID().toString()
            userDao.insert(UserEntity(id = id, email = email, passwordHash = hash(password)))
            session.signIn(id, remember)
            Result.success(Unit)
        }

    suspend fun login(email: String, password: String, remember: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val user = userDao.getByEmail(email) ?: return@withContext Result.failure(
                IllegalArgumentException("Неверный email/пароль")
            )
            if (user.passwordHash != hash(password)) return@withContext Result.failure(
                IllegalArgumentException("Неверный email/пароль")
            )
            session.signIn(user.id, remember)
            Result.success(Unit)
        }

    suspend fun logout() = session.signOut()
}