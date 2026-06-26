package com.example.real_estate_manager.data

import android.util.Log
import com.example.real_estate_manager.data.db.ReminderDao
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import com.example.real_estate_manager.data.model.ReminderType
import com.example.real_estate_manager.network.mappers.toEntity
import com.example.real_estate_manager.network.mappers.toRequestDto
import com.example.real_estate_manager.network.remote.RemoteReminderDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudReminderRepository @Inject constructor(
    private val local: RoomReminderRepository,
    private val reminderDao: ReminderDao,
    private val remote: RemoteReminderDataSource
) : ReminderRepository {
    private companion object {
        const val TAG = "ReminderSync"
    }
    private val syncMutexes = ConcurrentHashMap<String, Mutex>()

    override fun observeAll(userId: String): Flow<List<ReminderRuleEntity>> = channelFlow {
        launch(Dispatchers.IO) {
            runCatching { sync(userId) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "reminders sync failed before observeAll userId=$userId", error)
                }
        }
        local.observeAll(userId).collect { send(it) }
    }

    override fun observeForProperty(userId: String, propertyId: String): Flow<List<ReminderRuleEntity>> = channelFlow {
        launch(Dispatchers.IO) {
            runCatching { sync(userId, propertyId) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "reminders sync failed before observeForProperty userId=$userId propertyId=$propertyId", error)
                }
        }
        local.observeForProperty(userId, propertyId).collect { send(it) }
    }

    override suspend fun getDue(userId: String, nowMillis: Long): List<ReminderRuleEntity> =
        local.getDue(userId, nowMillis)

    override suspend fun upsert(rule: ReminderRuleEntity) =
        withContext(Dispatchers.IO) {
            val nextStatus = if (rule.syncStatus == SYNC_PENDING_CREATE) {
                SYNC_PENDING_CREATE
            } else {
                SYNC_PENDING_UPDATE
            }
            local.upsert(
                rule.copy(
                    syncStatus = nextStatus,
                    lastSyncError = null,
                    lastSyncAttemptAt = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            syncPendingReminders(rule.userId)
        }

    override suspend fun deleteById(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            val existing = reminderDao.getById(userId, id) ?: return@withContext
            if (existing.syncStatus == SYNC_PENDING_CREATE) {
                local.deleteById(userId, id)
                return@withContext
            }
            local.upsert(
                existing.copy(
                    enabled = false,
                    nextTriggerAt = Long.MAX_VALUE,
                    syncStatus = SYNC_PENDING_DELETE,
                    lastSyncError = null,
                    lastSyncAttemptAt = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            syncPendingReminders(userId)
        }

    override suspend fun createRule(
        userId: String,
        propertyId: String?,
        title: String,
        message: String?,
        type: ReminderType,
        scheduleMode: ReminderScheduleMode,
        oneTimeAt: Long?,
        rangeStartAt: Long?,
        rangeEndAt: Long?,
        dayOfMonth: Int?,
        rangeStartDay: Int?,
        rangeEndDay: Int?,
        repeatEveryDays: Int?,
        offsetDays: Int?,
        hour: Int,
        minute: Int,
        enabled: Boolean
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val draft = ReminderRuleEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            propertyId = propertyId,
            title = title,
            message = message,
            type = type.name,
            scheduleMode = scheduleMode.name,
            oneTimeAt = oneTimeAt,
            rangeStartAt = rangeStartAt,
            rangeEndAt = rangeEndAt,
            dayOfMonth = dayOfMonth,
            rangeStartDay = rangeStartDay,
            rangeEndDay = rangeEndDay,
            repeatEveryDays = repeatEveryDays,
            offsetDays = offsetDays,
            hour = hour,
            minute = minute,
            enabled = enabled,
            nextTriggerAt = now,
            lastFiredAt = null,
            createdAt = now,
            updatedAt = now,
            syncStatus = SYNC_PENDING_CREATE
        )
        val rule = draft.copy(nextTriggerAt = local.recomputeNextTriggerAt(draft, LocalDateTime.now()))
        local.upsert(rule)
        syncPendingReminders(userId)
    }

    override suspend fun createDefaultLeaseEndReminders(userId: String, propertyId: String) =
        local.createDefaultLeaseEndReminders(userId, propertyId)

    override suspend fun recomputeNextTriggerAt(rule: ReminderRuleEntity, now: LocalDateTime): Long =
        local.recomputeNextTriggerAt(rule, now)

    private suspend fun sync(userId: String, propertyId: String? = null) {
        withUserSyncLock(userId) {
            syncPendingRemindersLocked(userId)
            val pendingIds = reminderDao.pending(userId).map { it.id }.toSet()
            val remoteRules = remote.fetchAll(propertyId)
                .map { it.toEntity(userId) }
                .filter { it.id !in pendingIds }
                .map { normalizeRemoteRule(it) }
            if (propertyId == null) {
                if (remoteRules.isEmpty() && pendingIds.isEmpty()) {
                    deleteAllWithScheduledWork(userId)
                } else if (remoteRules.isNotEmpty()) {
                    upsertRemoteRules(remoteRules)
                    deleteMissingWithScheduledWork(userId, remoteRules.map { it.id })
                }
            } else if (remoteRules.isNotEmpty()) {
                upsertRemoteRules(remoteRules)
            }
        }
    }

    private suspend fun syncPendingReminders(userId: String) {
        withUserSyncLock(userId) {
            syncPendingRemindersLocked(userId)
        }
    }

    private suspend fun syncPendingRemindersLocked(userId: String) {
        reminderDao.pending(userId).forEach { pending ->
            try {
                val now = System.currentTimeMillis()
                reminderDao.upsert(pending.copy(lastSyncAttemptAt = now))
                when (pending.syncStatus) {
                    SYNC_PENDING_CREATE -> {
                        val created = normalizeRemoteRule(remote.create(pending.toRequestDto()).toEntity(userId))
                        if (created.id.isBlank()) {
                            reminderDao.upsert(
                                pending.copy(lastSyncError = "Сервер вернул пустой id", lastSyncAttemptAt = now)
                            )
                        } else {
                            if (created.id != pending.id) {
                                local.deleteById(userId, pending.id)
                            }
                            local.upsert(created)
                        }
                    }
                    SYNC_PENDING_UPDATE -> {
                        if (pending.isCompletedOneTime(now)) {
                            local.upsert(
                                pending.copy(
                                    enabled = false,
                                    nextTriggerAt = Long.MAX_VALUE,
                                    syncStatus = "SYNCED",
                                    lastSyncError = null,
                                    lastSyncAttemptAt = now
                                )
                            )
                            return@forEach
                        }
                        try {
                            val updated = normalizeRemoteRule(remote.update(pending.id, pending.toRequestDto()).toEntity(userId))
                            local.upsert(updated)
                        } catch (error: retrofit2.HttpException) {
                            if (error.code() == 404) {
                                reminderDao.upsert(
                                    pending.copy(
                                        syncStatus = SYNC_PENDING_CREATE,
                                        lastSyncError = "Серверная запись не найдена, будет создана заново",
                                        lastSyncAttemptAt = now
                                    )
                                )
                            } else {
                                throw error
                            }
                        }
                    }
                    SYNC_PENDING_DELETE -> {
                        remote.delete(pending.id)
                        local.deleteById(userId, pending.id)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (pending.syncStatus == SYNC_PENDING_DELETE && error.isRemoteNotFound()) {
                    local.deleteById(userId, pending.id)
                    Log.d(TAG, "Reminder tombstone cleaned: reminderId=${pending.id}")
                    return@forEach
                }
                reminderDao.upsert(
                    pending.copy(
                        lastSyncError = error.toSyncMessage(),
                        lastSyncAttemptAt = System.currentTimeMillis()
                    )
                )
                Log.w(TAG, "reminder sync failed id=${pending.id} status=${pending.syncStatus}", error)
            }
        }
    }

    private suspend fun <T> withUserSyncLock(userId: String, block: suspend () -> T): T =
        syncMutexes.getOrPut(userId) { Mutex() }.withLock { block() }

    private suspend fun normalizeRemoteRule(rule: ReminderRuleEntity): ReminderRuleEntity =
        ReminderSyncPlanner.normalizeRemoteRule(rule) { candidate, now ->
            local.recomputeNextTriggerAt(candidate, now)
        }

    private suspend fun upsertRemoteRules(rules: List<ReminderRuleEntity>) {
        rules.forEach { remoteRule ->
            val existing = reminderDao.getById(remoteRule.userId, remoteRule.id)
            val merged = if (remoteRule.isCompletedOneTime(System.currentTimeMillis()) && existing?.lastFiredAt != null) {
                remoteRule.copy(
                    lastFiredAt = existing.lastFiredAt,
                    updatedAt = maxOf(remoteRule.updatedAt, existing.updatedAt)
                )
            } else {
                remoteRule
            }
            local.upsert(merged)
        }
    }

    private suspend fun deleteMissingWithScheduledWork(userId: String, remoteIds: List<String>) {
        reminderDao.missingSyncedIds(userId, remoteIds).forEach { id ->
            local.deleteById(userId, id)
        }
    }

    private suspend fun deleteAllWithScheduledWork(userId: String) {
        reminderDao.ids(userId).forEach { id ->
            local.deleteById(userId, id)
        }
    }
}

private fun Throwable.toSyncMessage(): String =
    when (this) {
        is java.net.ConnectException -> "Сервер не запущен"
        is java.net.SocketTimeoutException -> "Сервер недоступен"
        is java.net.UnknownHostException -> "Неверный адрес сервера"
        is retrofit2.HttpException -> when (code()) {
            401 -> "Требуется повторный вход"
            403 -> "Доступ запрещен"
            in 500..599 -> "Ошибка сервера"
            else -> "Ошибка запроса: ${code()}"
        }
        is java.io.IOException -> "Сервер недоступен"
        else -> message ?: "Ошибка синхронизации"
    }

private fun Throwable.isRemoteNotFound(): Boolean {
    if (this is retrofit2.HttpException && code() == 404) return true
    val normalized = (message ?: "").lowercase()
    return "not found" in normalized ||
        "no " in normalized && " matches the given query" in normalized ||
        "не найден" in normalized
}

private fun ReminderRuleEntity.isCompletedOneTime(nowMillis: Long): Boolean =
    scheduleMode == ReminderScheduleMode.ONE_TIME.name &&
        !enabled &&
        (oneTimeAt ?: Long.MIN_VALUE) <= nowMillis &&
        nextTriggerAt == Long.MAX_VALUE

private const val SYNC_PENDING_CREATE = "PENDING_CREATE"
private const val SYNC_PENDING_UPDATE = "PENDING_UPDATE"
private const val SYNC_PENDING_DELETE = "PENDING_DELETE"
