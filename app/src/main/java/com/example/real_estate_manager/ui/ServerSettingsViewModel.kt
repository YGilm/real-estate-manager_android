package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.network.ConnectivityDiagnosticsRepository
import com.example.real_estate_manager.network.DiagnosticsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServerSettingsUiState(
    val serverUrl: String = "",
    val connectionStatus: String = "Не проверено",
    val apiVersion: String = "-",
    val serverTime: String = "-",
    val lastSuccessfulSyncMillis: Long? = null,
    val lastError: String? = null,
    val editable: Boolean = false,
    val checking: Boolean = false,
    val diagnostics: DiagnosticsSnapshot? = null
)

@HiltViewModel
class ServerSettingsViewModel @Inject constructor(
    private val diagnosticsRepository: ConnectivityDiagnosticsRepository
) : ViewModel() {
    private val localState = MutableStateFlow(ServerSettingsUiState())

    val uiState: StateFlow<ServerSettingsUiState> =
        combine(diagnosticsRepository.configFlow, localState) { config, state ->
            state.copy(
                serverUrl = state.serverUrl.ifBlank { config.serverUrl },
                lastSuccessfulSyncMillis = config.lastSuccessfulSyncMillis,
                lastError = config.lastError,
                editable = config.editable
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServerSettingsUiState())

    init {
        launchSafely {
            diagnosticsRepository.ensureDefaultServerUrl()
            val snapshot = diagnosticsRepository.diagnosticsSnapshot()
            localState.value = localState.value.copy(
                serverUrl = snapshot.serverUrl,
                diagnostics = snapshot
            )
        }
    }

    fun onServerUrlChange(value: String) {
        if (!uiState.value.editable) return
        localState.value = localState.value.copy(serverUrl = value)
    }

    fun save() {
        if (!uiState.value.editable) return
        launchSafely {
            diagnosticsRepository.saveServerUrl(uiState.value.serverUrl)
            refreshDiagnostics()
        }
    }

    fun checkConnection() {
        launchSafely {
            localState.value = localState.value.copy(checking = true)
            val result = diagnosticsRepository.checkConnection()
            localState.value = localState.value.copy(
                connectionStatus = result.message,
                apiVersion = result.health?.version ?: "-",
                serverTime = result.health?.server_time ?: "-",
                checking = false
            )
            refreshDiagnostics()
        }
    }

    fun startupCheck() {
        launchSafely {
            val result = diagnosticsRepository.checkConnection()
            localState.value = localState.value.copy(
                connectionStatus = result.message,
                apiVersion = result.health?.version ?: localState.value.apiVersion,
                serverTime = result.health?.server_time ?: localState.value.serverTime
            )
            refreshDiagnostics()
        }
    }

    private suspend fun refreshDiagnostics() {
        localState.value = localState.value.copy(
            diagnostics = diagnosticsRepository.diagnosticsSnapshot()
        )
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                localState.value = localState.value.copy(
                    checking = false,
                    connectionStatus = error.toUiErrorMessage(),
                    lastError = error.toUiErrorMessage()
                )
            }
        }
    }
}
