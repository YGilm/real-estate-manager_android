package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.auth.AuthRepository
import com.example.real_estate_manager.auth.SessionState
import com.example.real_estate_manager.auth.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val session: UserSession
) : ViewModel() {

    init {
        launchSafely {
            repo.restoreSessionFromToken()
        }
    }

    // --- то, что ожидает RealEstateNavigation.kt ---
    val sessionState: StateFlow<SessionState> =
        session.stateFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SessionState(userId = null, rememberMe = false, locked = false, expiresAtMillis = 0L, ttlMinutes = 15)
        )

    val ttlMinutes: StateFlow<Int> =
        session.ttlMinutesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 15)

    fun setTtl(minutes: Int) {
        launchSafely { session.setTtlMinutes(minutes) }
    }

    fun unlockByBiometricSuccess() {
        launchSafely { session.unlockAndExtend() }
    }

    fun unlockByPassword(password: String, onDone: (String?) -> Unit) {
        val uid = sessionState.value.userId
        if (uid.isNullOrBlank()) {
            onDone("Сессия недоступна, войдите заново")
            return
        }
        launchSafely(onError = onDone) {
            val result = repo.reauth(uid, password)
            onDone(result.exceptionOrNull()?.message)
        }
    }

    // --- то, что нужно для UI (email / профиль) ---
    val userId: StateFlow<String?> =
        sessionState.map { it.userId }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentEmail: StateFlow<String?> =
        userId.flatMapLatest { uid ->
            flow {
                emit(if (uid.isNullOrBlank()) null else repo.getEmailByUserId(uid))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun register(email: String, pass: String, remember: Boolean, onDone: (String?) -> Unit) {
        launchSafely(onError = onDone) {
            val result = repo.register(email.trim(), pass, remember)
            onDone(result.exceptionOrNull()?.message)
        }
    }

    fun login(email: String, pass: String, remember: Boolean, onDone: (String?) -> Unit) {
        launchSafely(onError = onDone) {
            val result = repo.login(email.trim(), pass, remember)
            onDone(result.exceptionOrNull()?.message)
        }
    }

    fun logout() {
        launchSafely { repo.logout() }
    }

    fun updateEmail(newEmail: String, onDone: (String?) -> Unit) {
        val uid = sessionState.value.userId
        if (uid.isNullOrBlank()) {
            onDone("Сессия не активна")
            return
        }
        launchSafely(onError = onDone) {
            val result = repo.updateEmail(uid, newEmail)
            onDone(result.exceptionOrNull()?.message)
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onDone: (String?) -> Unit) {
        val uid = sessionState.value.userId
        if (uid.isNullOrBlank()) {
            onDone("Сессия не активна")
            return
        }
        launchSafely(onError = onDone) {
            val result = repo.changePassword(uid, oldPassword, newPassword)
            onDone(result.exceptionOrNull()?.message)
        }
    }

    private fun launchSafely(
        onError: ((String?) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                onError?.invoke(error.toUiErrorMessage())
            }
        }
    }
}
