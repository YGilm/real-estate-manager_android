package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.db.NotificationDao
import com.example.real_estate_manager.data.db.NotificationEntity
import com.example.real_estate_manager.network.mappers.toEntity
import com.example.real_estate_manager.network.remote.RemoteNotificationDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudNotificationRepository @Inject constructor(
    private val dao: NotificationDao,
    private val remote: RemoteNotificationDataSource,
    private val reminderDao: com.example.real_estate_manager.data.db.ReminderDao,
    private val reminderRepository: ReminderRepository
) : NotificationRepository {
    override fun observeAll(userId: String): Flow<List<NotificationEntity>> =
        dao.observeAll(userId).onStart { runCatching { refresh(userId) } }

    override fun observeById(userId: String, id: String): Flow<NotificationEntity?> =
        dao.observeById(userId, id)

    override fun unreadCount(userId: String): Flow<Int> =
        dao.unreadCount(userId).onStart { runCatching { refresh(userId) } }

    override suspend fun refresh(userId: String) = withContext(Dispatchers.IO) {
        val entities = remote.fetchActive().map { it.toEntity(userId) }
        if (entities.isNotEmpty()) {
            dao.upsertAll(entities)
        }
    }

    override suspend fun markRead(userId: String, id: String) = withContext(Dispatchers.IO) {
        runCatching { remote.deactivate(id) }
        dao.markRead(userId, id)
    }

    override suspend fun markAllRead(userId: String) = withContext(Dispatchers.IO) {
        dao.markAllRead(userId)
    }

    override suspend fun delete(userId: String, id: String) = withContext(Dispatchers.IO) {
        dao.markDeleted(userId, id)
    }

    override suspend fun snoozeReminder(userId: String, reminderId: String, delayMillis: Long) =
        withContext(Dispatchers.IO) {
            val rule = reminderDao.getById(userId, reminderId) ?: return@withContext
            val now = System.currentTimeMillis()
            reminderRepository.upsert(ReminderSnoozePlanner.snooze(rule, now, delayMillis))
        }
}
