package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.db.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeAll(userId: String): Flow<List<NotificationEntity>>
    fun observeById(userId: String, id: String): Flow<NotificationEntity?>
    fun unreadCount(userId: String): Flow<Int>
    suspend fun refresh(userId: String)
    suspend fun markRead(userId: String, id: String)
    suspend fun markAllRead(userId: String)
    suspend fun delete(userId: String, id: String)
    suspend fun snoozeReminder(userId: String, reminderId: String, delayMillis: Long)

    fun observeActive(userId: String): Flow<List<NotificationEntity>> = observeAll(userId)
    fun countActive(userId: String): Flow<Int> = unreadCount(userId)
    suspend fun deactivate(userId: String, id: String) = markRead(userId, id)
    suspend fun deactivateAll(userId: String) = markAllRead(userId)
}
