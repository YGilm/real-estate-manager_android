package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.auth.UserSession
import com.example.real_estate_manager.data.NotificationRepository
import com.example.real_estate_manager.data.db.NotificationEntity
import com.example.real_estate_manager.data.db.ReminderDao
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val reminderDao: ReminderDao,
    session: UserSession
) : ViewModel() {

    private val userIdFlow: StateFlow<String?> =
        session.userIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val notifications: StateFlow<List<NotificationEntity>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else notificationRepository.observeAll(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(0) else notificationRepository.unreadCount(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val activeNotifications: StateFlow<List<NotificationEntity>> = notifications
    val activeCount: StateFlow<Int> = unreadCount

    fun notification(notificationId: String): StateFlow<NotificationDetailsState> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) {
                flowOf(NotificationDetailsState.Loading)
            } else {
                notificationRepository.observeById(uid, notificationId).map { notification ->
                    notification?.let(NotificationDetailsState::Found) ?: NotificationDetailsState.Removed
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationDetailsState.Loading)

    suspend fun reminderFor(notification: NotificationEntity): ReminderRuleEntity? =
        notification.relatedEntityId
            ?.takeIf { notification.relatedEntityType == "REMINDER" }
            ?.let { reminderDao.getById(notification.userId, it) }

    fun markRead(notification: NotificationEntity) {
        launchSafely {
            notificationRepository.markRead(notification.userId, notification.id)
        }
    }

    fun markAllRead() {
        val uid = userIdFlow.value ?: return
        launchSafely {
            notificationRepository.markAllRead(uid)
        }
    }

    fun delete(notification: NotificationEntity) {
        launchSafely {
            notificationRepository.delete(notification.userId, notification.id)
        }
    }

    fun snoozeReminder(notification: NotificationEntity, delayMillis: Long) {
        val reminderId = notification.relatedEntityId?.takeIf { notification.relatedEntityType == "REMINDER" } ?: return
        launchSafely {
            notificationRepository.snoozeReminder(notification.userId, reminderId, delayMillis)
        }
    }

    fun deactivate(notification: NotificationEntity) = markRead(notification)
    fun deactivateAll() = markAllRead()

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _errorMessage.value = error.toUiErrorMessage()
            }
        }
    }
}

sealed interface NotificationDetailsState {
    data object Loading : NotificationDetailsState
    data object Removed : NotificationDetailsState
    data class Found(val notification: NotificationEntity) : NotificationDetailsState
}
