@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.real_estate_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.real_estate_manager.data.db.NotificationEntity
import com.example.real_estate_manager.data.db.NotificationType
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.ui.NotificationDetailsState
import com.example.real_estate_manager.ui.NotificationsViewModel
import com.example.real_estate_manager.ui.components.EmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenNotification: (String) -> Unit,
    onOpenRelated: (type: String?, id: String?, propertyId: String?) -> Unit,
    vm: NotificationsViewModel = hiltViewModel()
) {
    val notifications by vm.notifications.collectAsState()
    val unreadCount by vm.unreadCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (unreadCount > 0) "Уведомления ($unreadCount)" else "Уведомления") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { vm.markAllRead() }) {
                            Text("Прочитать все")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp)
        ) {
            if (notifications.isEmpty()) {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    icon = Icons.Filled.Notifications,
                    title = "Уведомлений нет",
                    message = "Новые напоминания появятся здесь.",
                    primaryActionTitle = null,
                    onPrimaryAction = null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        NotificationCard(
                            notification = item,
                            onOpen = { onOpenNotification(item.id) },
                            onOpenRelated = {
                                onOpenRelated(
                                    item.relatedEntityType,
                                    item.relatedEntityId,
                                    item.actionPayload.propertyIdFromPayload()
                                )
                            },
                            onMarkRead = { vm.markRead(item) },
                            onDelete = { vm.delete(item) },
                            onSnooze = { vm.snoozeReminder(item, it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationDetailsScreen(
    notificationId: String,
    onBack: () -> Unit,
    onOpenRelated: (type: String?, id: String?, propertyId: String?) -> Unit,
    vm: NotificationsViewModel = hiltViewModel()
) {
    val notificationFlow = remember(notificationId, vm) { vm.notification(notificationId) }
    val notificationState by notificationFlow.collectAsState()
    val item = (notificationState as? NotificationDetailsState.Found)?.notification
    val reminder by produceState<ReminderRuleEntity?>(initialValue = null, item) {
        value = item?.let { vm.reminderFor(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Уведомление") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when (notificationState) {
            NotificationDetailsState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Загрузка...")
                }
                return@Scaffold
            }
            NotificationDetailsState.Removed -> {
                EmptyState(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    icon = Icons.Filled.Notifications,
                    title = "Notification was removed",
                    message = null,
                    primaryActionTitle = null,
                    onPrimaryAction = null
                )
                return@Scaffold
            }
            is NotificationDetailsState.Found -> Unit
        }
        val item = item ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (item.isRead) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssistChip(onClick = {}, label = { Text(notificationTypeTitle(item.type)) })
                            AssistChip(onClick = {}, label = { Text(if (item.isRead) "Прочитано" else "Новое") })
                        }
                        Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        item.message?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            formatNotificationTime(item.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (item.type == NotificationType.REMINDER.name) {
                item {
                    ReminderInfoCard(reminder)
                }
            }

            item {
                NotificationActions(
                    notification = item,
                    onOpenRelated = {
                        onOpenRelated(
                            item.relatedEntityType,
                            item.relatedEntityId,
                            reminder?.propertyId ?: item.actionPayload.propertyIdFromPayload()
                        )
                    },
                    onMarkRead = { vm.markRead(item) },
                    onDelete = {
                        vm.delete(item)
                        onBack()
                    },
                    onSnooze = { vm.snoozeReminder(item, it) }
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationEntity,
    onOpen: () -> Unit,
    onOpenRelated: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: (Long) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (notification.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(notification.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    notification.message?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${notificationTypeTitle(notification.type)} • ${formatNotificationTime(notification.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onOpen) { Text("Открыть") }
                if (notification.relatedEntityId != null) {
                    TextButton(onClick = onOpenRelated) { Text(if (notification.type == NotificationType.REMINDER.name) "Напоминание" else "Связь") }
                }
                if (!notification.isRead) {
                    IconButton(onClick = onMarkRead) {
                        Icon(Icons.Filled.Done, contentDescription = "Прочитано")
                    }
                }
                if (notification.type == NotificationType.REMINDER.name) {
                    IconButton(onClick = { onSnooze(10 * 60_000L) }) {
                        Icon(Icons.Filled.Schedule, contentDescription = "Отложить")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}

@Composable
private fun ReminderInfoCard(reminder: ReminderRuleEntity?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Напоминание", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (reminder == null) {
                Text("Связанное напоминание не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(reminder.title)
                reminder.message?.takeIf { it.isNotBlank() }?.let { Text(it) }
                Text(
                    "Следующее срабатывание: ${formatNotificationTime(reminder.nextTriggerAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationActions(
    notification: NotificationEntity,
    onOpenRelated: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!notification.isRead) {
            Button(onClick = onMarkRead, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Done, contentDescription = null)
                Text("Отметить прочитанным", modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (notification.relatedEntityId != null) {
            OutlinedButton(onClick = onOpenRelated, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Text(if (notification.type == NotificationType.REMINDER.name) "Открыть напоминание" else "Открыть связанный объект", modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (notification.type == NotificationType.REMINDER.name) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onSnooze(10 * 60_000L) }, modifier = Modifier.weight(1f)) { Text("+10 мин") }
                OutlinedButton(onClick = { onSnooze(60 * 60_000L) }, modifier = Modifier.weight(1f)) { Text("+1 час") }
            }
            OutlinedButton(onClick = { onSnooze(24 * 60 * 60_000L) }, modifier = Modifier.fillMaxWidth()) {
                Text("Завтра")
            }
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Text("Удалить уведомление", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun notificationTypeTitle(type: String): String =
    when (type) {
        NotificationType.REMINDER.name -> "Напоминание"
        NotificationType.PROPERTY.name -> "Объект"
        NotificationType.PAYMENT.name -> "Платеж"
        NotificationType.SYNC_ERROR.name -> "Синхронизация"
        else -> "Система"
    }

private fun formatNotificationTime(value: Long): String {
    if (value == Long.MAX_VALUE) return "не запланировано"
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(formatter)
}

private fun String?.propertyIdFromPayload(): String? =
    this
        ?.split("&")
        ?.mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
        ?.firstOrNull { it.first == "propertyId" }
        ?.second
