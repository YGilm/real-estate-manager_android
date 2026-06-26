package com.example.real_estate_manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.real_estate_manager.ui.ServerSettingsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServerSettingsScreen(
    onBack: () -> Unit,
    vm: ServerSettingsViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    var showDiagnostics by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Для разработчиков → Сервер") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Сервер", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.serverUrl,
                        onValueChange = vm::onServerUrlChange,
                        enabled = state.editable,
                        label = { Text("Текущий адрес сервера") },
                        singleLine = true,
                        placeholder = { Text("http://10.0.2.2:8000/") },
                        supportingText = {
                            Text(
                                if (state.editable) {
                                    "DEBUG: адрес можно изменить для диагностики."
                                } else {
                                    "RELEASE: используется production-сервер."
                                }
                            )
                        }
                    )
                    InfoRow("Статус соединения", state.connectionStatus)
                    InfoRow("Версия API", state.apiVersion)
                    InfoRow("Последняя успешная синхронизация", formatMillis(state.lastSuccessfulSyncMillis))
                    InfoRow("Последняя ошибка", state.lastError ?: "-")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(min = 160.dp),
                            onClick = vm::checkConnection,
                            enabled = !state.checking
                        ) {
                            Icon(Icons.Filled.WifiFind, contentDescription = null)
                            Text(if (state.checking) "Проверка..." else "Проверить соединение")
                        }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(min = 120.dp),
                            onClick = vm::save,
                            enabled = state.editable
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Text("Сохранить")
                        }
                    }
                }
            }

            TextButton(onClick = { showDiagnostics = !showDiagnostics }) {
                Text(if (showDiagnostics) "Скрыть диагностику" else "Показать диагностику")
            }

            if (showDiagnostics) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Диагностика", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        InfoRow("Current BASE_URL", state.diagnostics?.serverUrl ?: state.serverUrl)
                        InfoRow("Current user", state.diagnostics?.currentUser ?: "-")
                        InfoRow("JWT token expiration", state.diagnostics?.tokenExpiresAt ?: "-")
                        InfoRow("Last sync timestamp", formatMillis(state.diagnostics?.lastSuccessfulSyncMillis))
                        InfoRow("Last sync result", state.diagnostics?.lastError ?: "ok")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatMillis(value: Long?): String {
    if (value == null) return "-"
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(value))
}
