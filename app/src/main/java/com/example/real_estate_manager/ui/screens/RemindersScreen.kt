@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.example.real_estate_manager.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.model.Property
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import com.example.real_estate_manager.data.model.ReminderType
import com.example.real_estate_manager.ui.RemindersViewModel
import com.example.real_estate_manager.ui.components.EmptyState
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RemindersScreen(
    propertyId: String?,
    focusReminderId: String? = null,
    onBack: () -> Unit,
    vm: RemindersViewModel = hiltViewModel()
) {
    val rules by vm.rules(propertyId).collectAsState(initial = emptyList())
    val properties by vm.properties.collectAsState()
    val property = properties.firstOrNull { it.id == propertyId }
    var showCreate by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<ReminderRuleEntity?>(null) }
    var handledFocusReminderId by remember(focusReminderId) { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) { requestNotificationsPermission() }
    LaunchedEffect(focusReminderId, rules) {
        val id = focusReminderId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (handledFocusReminderId == id) return@LaunchedEffect
        rules.firstOrNull { it.id == id }?.let { rule ->
            editingRule = rule
            handledFocusReminderId = id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(property?.name ?: "Напоминания") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                requestNotificationsPermission()
                showCreate = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp)
        ) {
            if (rules.isEmpty()) {
                EmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    icon = Icons.Filled.Notifications,
                    title = "Нет напоминаний.",
                    message = "Добавьте первое напоминание для этого объекта.",
                    primaryActionTitle = "Добавить напоминание",
                    onPrimaryAction = {
                        requestNotificationsPermission()
                        showCreate = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rules, key = { it.id }) { rule ->
                        ReminderRuleCard(
                            rule = rule,
                            propertyName = properties.firstOrNull { it.id == rule.propertyId }?.name,
                            onClick = { editingRule = rule },
                            onEnabledChange = {
                                requestNotificationsPermission()
                                vm.setEnabled(rule, it)
                            },
                            onDelete = { vm.delete(rule) }
                        )
                    }
                }
            }
        }
    }

    if (showCreate || editingRule != null) {
        CreateReminderDialog(
            rule = editingRule,
            initialPropertyId = propertyId,
            property = property,
            onDismiss = {
                showCreate = false
                editingRule = null
            },
            onCreate = { selectedPropertyId, title, message, mode, oneTimeAt, rangeStartAt, rangeEndAt, day, repeat, hour, minute, enabled ->
                vm.createRule(
                    propertyId = selectedPropertyId,
                    title = title,
                    message = message,
                    type = ReminderType.UTILITIES,
                    scheduleMode = mode,
                    oneTimeAt = oneTimeAt,
                    rangeStartAt = rangeStartAt,
                    rangeEndAt = rangeEndAt,
                    dayOfMonth = day,
                    rangeStartDay = null,
                    rangeEndDay = null,
                    repeatEveryDays = repeat,
                    offsetDays = null,
                    hour = hour,
                    minute = minute,
                    enabled = enabled
                )
                showCreate = false
            },
            onUpdate = { rule, title, message, mode, oneTimeAt, rangeStartAt, rangeEndAt, day, repeat, hour, minute, enabled ->
                vm.updateRule(
                    rule = rule,
                    title = title,
                    message = message,
                    scheduleMode = mode,
                    oneTimeAt = oneTimeAt,
                    rangeStartAt = rangeStartAt,
                    rangeEndAt = rangeEndAt,
                    dayOfMonth = day,
                    repeatEveryDays = repeat,
                    hour = hour,
                    minute = minute,
                    enabled = enabled
                )
                editingRule = null
            }
        )
    }
}

@Composable
private fun ReminderRuleCard(
    rule: ReminderRuleEntity,
    propertyName: String?,
    onClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = rule.title.ifBlank { "Напоминание" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (!rule.message.isNullOrBlank()) {
                    Text(
                        text = rule.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = propertyName ?: "Без привязки",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Следующее: ${formatTrigger(rule.nextTriggerAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                reminderSyncStatusText(rule)?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rule.lastSyncError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
            Switch(checked = rule.enabled, onCheckedChange = onEnabledChange)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
    }
}

private fun reminderSyncStatusText(rule: ReminderRuleEntity): String? =
    if (rule.lastSyncError != null) {
        "Ошибка синхронизации"
    } else {
        when (rule.syncStatus) {
            "PENDING_CREATE",
            "PENDING_UPDATE",
            "PENDING_DELETE" -> "Ожидает синхронизации"
            else -> null
        }
    }

@Composable
private fun CreateReminderDialog(
    rule: ReminderRuleEntity?,
    initialPropertyId: String?,
    property: Property?,
    onDismiss: () -> Unit,
    onCreate: (String?, String, String?, ReminderScheduleMode, Long?, Long?, Long?, Int?, Int?, Int, Int, Boolean) -> Unit,
    onUpdate: (ReminderRuleEntity, String, String?, ReminderScheduleMode, Long?, Long?, Long?, Int?, Int?, Int, Int, Boolean) -> Unit
) {
    val minimumDateTime = remember(rule?.id) { nearestAllowedDateTime() }
    val initialMode = remember(rule?.id) {
        rule?.scheduleMode
            ?.let { runCatching { enumValueOf<ReminderScheduleMode>(it) }.getOrNull() }
            ?: ReminderScheduleMode.ONE_TIME
    }
    val initialOneTime = remember(rule?.id) {
        rule?.oneTimeAt?.takeIf { it > 0 }?.toLocalDateTime()
            ?.takeUnless { it.isBefore(minimumDateTime) }
            ?: minimumDateTime
    }
    val initialRangeStart = remember(rule?.id) {
        val savedEnd = rule?.rangeEndAt?.takeIf { it > 0 }?.toLocalDate()
        rule?.rangeStartAt?.takeIf { it > 0 }?.toLocalDate()
            ?.takeUnless { savedEnd?.isBefore(LocalDate.now()) == true }
            ?: LocalDate.now()
    }
    val initialRangeEnd = remember(rule?.id) {
        rule?.rangeEndAt?.takeIf { it > 0 }?.toLocalDate()
            ?.takeUnless { it.isBefore(LocalDate.now()) }
            ?: initialRangeStart.plusDays(7)
    }
    val today = LocalDate.now()
    var mode by remember(rule?.id) { mutableStateOf(initialMode) }
    var title by remember(rule?.id) { mutableStateOf(rule?.title.orEmpty()) }
    var message by remember(rule?.id) { mutableStateOf(rule?.message.orEmpty()) }
    var date by remember(rule?.id) { mutableStateOf(initialOneTime.toLocalDate()) }
    var rangeStart by remember(rule?.id) { mutableStateOf(initialRangeStart) }
    var rangeEnd by remember(rule?.id) { mutableStateOf(initialRangeEnd) }
    var monthlyDate by remember(rule?.id) {
        mutableStateOf(monthlyPickerDate(rule?.dayOfMonth ?: today.dayOfMonth))
    }
    var repeatEveryDays by remember(rule?.id) { mutableIntStateOf((rule?.repeatEveryDays ?: 1).coerceIn(1, 7)) }
    var enabled by remember(rule?.id) { mutableStateOf(rule?.enabled ?: true) }
    var hour by remember(rule?.id) {
        mutableIntStateOf(if (rule?.oneTimeAt?.toLocalDateTime()?.isBefore(minimumDateTime) == true) initialOneTime.hour else rule?.hour ?: initialOneTime.hour)
    }
    var minute by remember(rule?.id) {
        mutableIntStateOf(if (rule?.oneTimeAt?.toLocalDateTime()?.isBefore(minimumDateTime) == true) initialOneTime.minute else rule?.minute ?: initialOneTime.minute)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var pickingDate by remember { mutableStateOf<DateTarget?>(null) }
    var pickingTime by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    val wasShiftedToFuture = rule != null && (
            rule.oneTimeAt?.toLocalDateTime()?.isBefore(minimumDateTime) == true ||
                    rule.rangeEndAt?.toLocalDate()?.isBefore(LocalDate.now()) == true
            )
    val hasUnsavedChanges = title.isNotBlank() ||
            message.isNotBlank() ||
            mode != ReminderScheduleMode.ONE_TIME ||
            date != LocalDate.now() ||
            rangeStart != LocalDate.now() ||
            rangeEnd != LocalDate.now().plusDays(7) ||
            monthlyDate != LocalDate.now() ||
            repeatEveryDays != 1 ||
            enabled != true ||
            hour != minimumDateTime.hour ||
            minute != minimumDateTime.minute

    fun requestClose() {
        if (hasUnsavedChanges) confirmClose = true else onDismiss()
    }

    AlertDialog(
        onDismissRequest = { if (hasUnsavedChanges) confirmClose = true },
        title = { Text(if (rule == null) "Новое напоминание" else "Редактировать напоминание") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (property != null) {
                    Text("Объект: ${property.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (wasShiftedToFuture) {
                    Text(
                        text = "Прошедшая дата сдвинута на ближайшее доступное время.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Заголовок") },
                    isError = error != null && title.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null && title.isBlank()) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Комментарий") },
                    placeholder = { Text("Текст уведомления") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Включено", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                ScheduleDropdown(mode = mode, onModeChange = { mode = it })

                when (mode) {
                    ReminderScheduleMode.ONE_TIME -> {
                        DateRow("Дата", date) { pickingDate = DateTarget.ONE_TIME }
                        ValueRow("Установить время", "%02d:%02d".format(hour, minute)) { pickingTime = true }
                    }
                    ReminderScheduleMode.DAILY -> Text("Ежедневно в выбранное время")
                    ReminderScheduleMode.MONTHLY -> DateRow("День месяца", monthlyDate) { pickingDate = DateTarget.MONTHLY }
                    ReminderScheduleMode.DATE_RANGE -> {
                        DateRow("С", rangeStart) { pickingDate = DateTarget.RANGE_START }
                        DateRow("По", rangeEnd) { pickingDate = DateTarget.RANGE_END }
                        IntervalDropdown(intervalDays = repeatEveryDays, onIntervalChange = { repeatEveryDays = it })
                    }
                    else -> Unit
                }

                if (mode != ReminderScheduleMode.ONE_TIME) {
                    ValueRow("Установить время", "%02d:%02d".format(hour, minute)) { pickingTime = true }
                }
                if (error != null && title.isNotBlank()) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isBlank()) {
                        error = "Заголовок обязателен"
                        return@TextButton
                    }
                    val minAllowed = nearestAllowedDateTime()
                    val oneTimeAt = if (mode == ReminderScheduleMode.ONE_TIME) date.atTime(hour, minute).toMillis() else null
                    val rangeStartAt = if (mode == ReminderScheduleMode.DATE_RANGE) rangeStart.atStartOfDay().toMillis() else null
                    val rangeEndAt = if (mode == ReminderScheduleMode.DATE_RANGE) rangeEnd.atStartOfDay().toMillis() else null
                    if (mode == ReminderScheduleMode.ONE_TIME && date.atTime(hour, minute).isBefore(minAllowed)) {
                        error = "Нельзя выбрать прошедшее время"
                        return@TextButton
                    }
                    if (mode == ReminderScheduleMode.DATE_RANGE && rangeEnd.isBefore(rangeStart)) {
                        error = "Дата окончания не может быть раньше начала"
                        return@TextButton
                    }
                    if (mode == ReminderScheduleMode.DATE_RANGE && rangeEnd.atTime(hour, minute).isBefore(minAllowed)) {
                        error = "Нельзя выбрать прошедшее время"
                        return@TextButton
                    }
                    val cleanMessage = message.trim().takeIf { it.isNotBlank() }
                    val dayOfMonth = if (mode == ReminderScheduleMode.MONTHLY) monthlyDate.dayOfMonth else null
                    val repeat = if (mode == ReminderScheduleMode.DATE_RANGE) repeatEveryDays else null
                    if (rule != null) {
                        onUpdate(
                            rule,
                            cleanTitle,
                            cleanMessage,
                            mode,
                            oneTimeAt,
                            rangeStartAt,
                            rangeEndAt,
                            dayOfMonth,
                            repeat,
                            hour,
                            minute,
                            enabled
                        )
                        return@TextButton
                    }
                    onCreate(
                        initialPropertyId,
                        cleanTitle,
                        cleanMessage,
                        mode,
                        oneTimeAt,
                        rangeStartAt,
                        rangeEndAt,
                        dayOfMonth,
                        repeat,
                        hour,
                        minute,
                        enabled
                    )
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = { requestClose() }) { Text("Отмена") } }
    )

    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text("Несохранённые изменения будут потеряны") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClose = false
                    onDismiss()
                }) { Text("Сбросить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClose = false }) { Text("Продолжить") }
            }
        )
    }

    if (pickingDate != null) {
        ReminderDatePickerDialog(
            initialDate = when (pickingDate) {
                DateTarget.ONE_TIME -> date
                DateTarget.MONTHLY -> monthlyDate
                DateTarget.RANGE_START -> rangeStart
                DateTarget.RANGE_END -> rangeEnd
                null -> LocalDate.now()
            },
            minDate = when (pickingDate) {
                DateTarget.RANGE_END -> maxOf(LocalDate.now(), rangeStart)
                else -> LocalDate.now()
            },
            onDismiss = { pickingDate = null },
            onSelected = {
                when (pickingDate) {
                    DateTarget.ONE_TIME -> date = it
                    DateTarget.MONTHLY -> monthlyDate = it
                    DateTarget.RANGE_START -> rangeStart = it
                    DateTarget.RANGE_END -> rangeEnd = it
                    null -> Unit
                }
                pickingDate = null
            }
        )
    }

    if (pickingTime) {
        TimeWheelDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { pickingTime = false },
            onSelected = { h, m ->
                hour = h
                minute = m
                pickingTime = false
            }
        )
    }
}

@Composable
private fun ScheduleDropdown(
    mode: ReminderScheduleMode,
    onModeChange: (ReminderScheduleMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val modes = listOf(
        ReminderScheduleMode.ONE_TIME,
        ReminderScheduleMode.DAILY,
        ReminderScheduleMode.MONTHLY,
        ReminderScheduleMode.DATE_RANGE
    )
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = scheduleTitle(mode),
            onValueChange = {},
            label = { Text("Расписание") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            modes.forEach { item ->
                DropdownMenuItem(
                    text = { Text(scheduleTitle(item)) },
                    onClick = {
                        onModeChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun IntervalDropdown(
    intervalDays: Int,
    onIntervalChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = intervalTitle(intervalDays),
            onValueChange = {},
            label = { Text("Интервал") },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..7).forEach { days ->
                DropdownMenuItem(
                    text = { Text(intervalTitle(days)) },
                    onClick = {
                        onIntervalChange(days)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DateRow(label: String, date: LocalDate, onClick: () -> Unit) {
    ValueRow(label, date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), onClick)
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReminderDatePickerDialog(
    initialDate: LocalDate,
    minDate: LocalDate,
    onDismiss: () -> Unit,
    onSelected: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay().toMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .isBefore(minDate)
                    .not()
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: initialDate.atStartOfDay().toMillis()
                    onSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                }
            ) { Text("Ок") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    ) { DatePicker(state = state) }
}

@Composable
private fun TimeWheelDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onSelected: (Int, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = initialHour.coerceIn(0, 23))
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinute.coerceIn(0, 59))
    val selectedHour by remember { derivedStateOf { hourState.firstVisibleItemIndex.coerceIn(0, 23) } }
    val selectedMinute by remember { derivedStateOf { minuteState.firstVisibleItemIndex.coerceIn(0, 59) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Установите время",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    NumberWheel(24, hourState, Modifier.weight(1f))
                    NumberWheel(60, minuteState, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelected(selectedHour, selectedMinute)
                }
            ) { Text("Ок") }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch {
                    hourState.scrollToItem(initialHour.coerceIn(0, 23))
                    minuteState.scrollToItem(initialMinute.coerceIn(0, 59))
                }
                onDismiss()
            }) { Text("Отмена") }
        }
    )
}

@Composable
private fun NumberWheel(count: Int, state: androidx.compose.foundation.lazy.LazyListState, modifier: Modifier) {
    val selected by remember { derivedStateOf { state.firstVisibleItemIndex.coerceIn(0, count - 1) } }
    Box(modifier = modifier.height(144.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            contentPadding = PaddingValues(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items((0 until count).toList()) { value ->
                val isSelected = value == selected
                Text(
                    text = "%02d".format(value),
                    modifier = Modifier
                        .height(48.dp)
                        .padding(vertical = 10.dp)
                        .alpha(if (isSelected) 1f else 0.38f),
                    style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    RoundedCornerShape(12.dp)
                )
        )
    }
}

private enum class DateTarget { ONE_TIME, MONTHLY, RANGE_START, RANGE_END }

private fun reminderTypeTitle(type: String): String =
    when (enumValueOf<ReminderType>(type)) {
        ReminderType.READINGS -> "Подача показаний"
        ReminderType.UTILITIES -> "Оплата коммуналки"
        ReminderType.INTERNET -> "Оплата интернета"
        ReminderType.RENT -> "Арендная плата"
        ReminderType.LEASE_END -> "Окончание аренды"
    }

private fun scheduleTitle(mode: ReminderScheduleMode): String =
    when (mode) {
        ReminderScheduleMode.ONE_TIME -> "Один раз"
        ReminderScheduleMode.DAILY -> "Ежедневно"
        ReminderScheduleMode.MONTHLY -> "Ежемесячно"
        ReminderScheduleMode.DATE_RANGE -> "Период"
        ReminderScheduleMode.DAY_OF_MONTH -> "День месяца"
        ReminderScheduleMode.DATE_RANGE_MONTHLY -> "Период месяца"
        ReminderScheduleMode.RELATIVE_TO_LEASE_END -> "До окончания аренды"
    }

private fun intervalTitle(days: Int): String =
    if (days <= 1) "Ежедневно" else "Каждые $days дня"

private fun formatTrigger(value: Long): String {
    if (value == Long.MAX_VALUE) return "не запланировано"
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    return Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(formatter)
}

private fun nearestAllowedDateTime(): LocalDateTime {
    val now = LocalDateTime.now()
    return if (now.second > 0 || now.nano > 0) {
        now.plusMinutes(1).withSecond(0).withNano(0)
    } else {
        now.withSecond(0).withNano(0)
    }
}

private fun monthlyPickerDate(dayOfMonth: Int): LocalDate {
    val today = LocalDate.now()
    val thisMonthDay = today.withDayOfMonth(dayOfMonth.coerceIn(1, today.lengthOfMonth()))
    if (!thisMonthDay.isBefore(today)) return thisMonthDay
    val nextMonth = today.plusMonths(1)
    return nextMonth.withDayOfMonth(dayOfMonth.coerceIn(1, nextMonth.lengthOfMonth()))
}

private fun LocalDateTime.toMillis(): Long =
    atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
