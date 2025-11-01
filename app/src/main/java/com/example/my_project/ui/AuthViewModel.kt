package com.example.my_project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_project.auth.AuthRepository
import com.example.my_project.auth.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
    session: UserSession
) : ViewModel() {

    val userId: StateFlow<String?> = session.userIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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
}