package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.auth.UserSession
import com.example.real_estate_manager.data.db.NotificationDao
import com.example.real_estate_manager.data.db.NotificationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
    session: UserSession
) : ViewModel() {

    private val userIdFlow: StateFlow<String?> =
        session.userIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeNotifications: StateFlow<List<NotificationEntity>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else notificationDao.observeActive(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCount: StateFlow<Int> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(0) else notificationDao.countActive(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun deactivate(notification: NotificationEntity) {
        viewModelScope.launch {
            notificationDao.deactivate(notification.userId, notification.id)
        }
    }

    fun deactivateAll() {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            notificationDao.deactivateAll(uid)
        }
    }
}
