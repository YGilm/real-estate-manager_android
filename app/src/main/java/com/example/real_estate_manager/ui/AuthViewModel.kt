package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.auth.AuthRepository
import com.example.real_estate_manager.auth.SessionState
import com.example.real_estate_manager.auth.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val session: UserSession
) : ViewModel() {

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
        viewModelScope.launch { session.setTtlMinutes(minutes) }
    }

    fun unlockByBiometricSuccess() {
        viewModelScope.launch { session.unlockAndExtend() }
    }

    fun unlockByPassword(password: String, onDone: (String?) -> Unit) {
        val uid = sessionState.value.userId
        if (uid.isNullOrBlank()) {
            onDone("Сессия недоступна, войдите заново")
            return
        }
        viewModelScope.launch {
            val res = repo.reauth(uid, password)
            onDone(res.exceptionOrNull()?.message)
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
        viewModelScope.launch {
            val res = repo.register(email.trim(), pass, remember)
            onDone(res.exceptionOrNull()?.message)
        }
    }

    fun login(email: String, pass: String, remember: Boolean, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val res = repo.login(email.trim(), pass, remember)
            onDone(res.exceptionOrNull()?.message)
        }
    }

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }

    fun updateEmail(newEmail: String, onDone: (String?) -> Unit) {
        val uid = sessionState.value.userId
        if (uid.isNullOrBlank()) {
            onDone("Сессия не активна")
            return
        }
        viewModelScope.launch {
            val res = repo.updateEmail(uid, newEmail)
            onDone(res.exceptionOrNull()?.message)
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, onDone: (String?) -> Unit) {
        val uid = sessionState.value.userId
        if (uid.isNullOrBlank()) {
            onDone("Сессия не активна")
            return
        }
        viewModelScope.launch {
            val res = repo.changePassword(uid, oldPassword, newPassword)
            onDone(res.exceptionOrNull()?.message)
        }
    }
}