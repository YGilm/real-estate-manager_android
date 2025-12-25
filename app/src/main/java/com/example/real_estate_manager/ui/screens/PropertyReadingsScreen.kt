@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.real_estate_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.real_estate_manager.data.model.FieldEntry
import com.example.real_estate_manager.data.model.ProviderWidget
import com.example.real_estate_manager.data.model.WidgetField
import com.example.real_estate_manager.data.model.WidgetFieldType
import com.example.real_estate_manager.ui.CustomWidgetField
import com.example.real_estate_manager.ui.FieldEntryInput
import com.example.real_estate_manager.ui.PropertyReadingsViewModel
import com.example.real_estate_manager.ui.WidgetTemplate
import com.example.real_estate_manager.ui.util.moneyFormatPlain
import com.example.real_estate_manager.ui.util.monthName
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

private data class FieldInputState(
    val numberText: String = "",
    val textValue: String = "",
    val statusPaid: Boolean = false
)

private data class EditableField(
    val id: String,
    val name: String,
    val fieldType: WidgetFieldType
)

private data class CustomWidgetDraft(
    val widgetId: String?,
    val title: String,
    val fields: List<EditableField>
)

@Composable
fun PropertyReadingsScreen(
    propertyId: String,
    onBack: () -> Unit
) {
    val vm: PropertyReadingsViewModel = hiltViewModel()
    LaunchedEffect(propertyId) { vm.bind(propertyId) }

    val uiState by vm.uiState.collectAsState()
    val now = remember { LocalDate.now() }

    var addDialogOpen by remember { mutableStateOf(false) }
    var mosenergoDialogOpen by remember { mutableStateOf(false) }
    var entryWidget by remember { mutableStateOf<ProviderWidget?>(null) }
    var historyWidget by remember { mutableStateOf<ProviderWidget?>(null) }
    var editWidget by remember { mutableStateOf<ProviderWidget?>(null) }
    var deleteWidget by remember { mutableStateOf<ProviderWidget?>(null) }
    var customDialog by remember { mutableStateOf<CustomWidgetDraft?>(null) }

    val fieldsByWidget = uiState.fields.groupBy { it.widgetId }
    val entriesByField = uiState.entries.groupBy { it.fieldId }

    if (addDialogOpen) {
        AlertDialog(
            onDismissRequest = { addDialogOpen = false },
            title = { Text("Добавить услугу") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TemplateOptionCard(
                        title = "Мосэнерго",
                        subtitle = "Счётчики электроэнергии"
                    ) {
                        addDialogOpen = false
                        mosenergoDialogOpen = true
                    }
                    TemplateOptionCard(
                        title = "Вода",
                        subtitle = "ХВС и ГВС"
                    ) {
                        addDialogOpen = false
                        vm.addTemplate(WidgetTemplate.UK_WATER)
                    }
                    TemplateOptionCard(
                        title = "Конструктор",
                        subtitle = "Произвольные поля"
                    ) {
                        addDialogOpen = false
                        customDialog = CustomWidgetDraft(
                            widgetId = null,
                            title = "",
                            fields = listOf(
                                EditableField(
                                    id = UUID.randomUUID().toString(),
                                    name = "Поле",
                                    fieldType = WidgetFieldType.TEXT
                                )
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { addDialogOpen = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (mosenergoDialogOpen) {
        AlertDialog(
            onDismissRequest = { mosenergoDialogOpen = false },
            title = { Text("Мосэнерго") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TemplateOptionCard(
                        title = "Однотарифный",
                        subtitle = "Одно поле показаний"
                    ) {
                        mosenergoDialogOpen = false
                        vm.addTemplate(WidgetTemplate.MOSENERGO_SINGLE)
                    }
                    TemplateOptionCard(
                        title = "День/ночь",
                        subtitle = "Два поля показаний"
                    ) {
                        mosenergoDialogOpen = false
                        vm.addTemplate(WidgetTemplate.MOSENERGO_DAYNIGHT)
                    }
                    TemplateOptionCard(
                        title = "Трёхтарифный",
                        subtitle = "Т1, Т2, Т3"
                    ) {
                        mosenergoDialogOpen = false
                        vm.addTemplate(WidgetTemplate.MOSENERGO_THREETARIFF)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mosenergoDialogOpen = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    customDialog?.let { draft ->
        var title by remember(draft.widgetId) { mutableStateOf(draft.title) }
        var fields by remember(draft.widgetId) { mutableStateOf(draft.fields) }
        var typeMenuFor by remember(draft.widgetId) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { customDialog = null },
            title = { Text(if (draft.widgetId == null) "Конструктор" else "Редактировать услугу") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    fields.forEachIndexed { index, field ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = field.name,
                                onValueChange = { value ->
                                    fields = fields.toMutableList().also {
                                        it[index] = field.copy(name = value)
                                    }
                                },
                                label = { Text("Поле ${index + 1}") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Тип: ${fieldTypeLabel(field.fieldType)}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    androidx.compose.foundation.layout.Box {
                                        TextButton(onClick = { typeMenuFor = field.id }) {
                                            Text("Выбрать")
                                        }
                                        DropdownMenu(
                                            expanded = typeMenuFor == field.id,
                                            onDismissRequest = { typeMenuFor = null }
                                        ) {
                                            fieldTypeOptions().forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(fieldTypeLabel(option)) },
                                                    onClick = {
                                                        fields = fields.toMutableList().also {
                                                            it[index] = field.copy(fieldType = option)
                                                        }
                                                        typeMenuFor = null
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = {
                                        fields = fields.toMutableList().also { list ->
                                            list.removeAt(index)
                                        }
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Удалить поле")
                                    }
                                }
                            }
                        }
                    }

                    TextButton(onClick = {
                        fields = fields + EditableField(
                            id = UUID.randomUUID().toString(),
                            name = "Поле",
                            fieldType = WidgetFieldType.TEXT
                        )
                    }) {
                        Text("+ Добавить поле")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanedTitle = title.trim().ifBlank { "Конструктор" }
                    val cleanedFields = fields
                        .filter { it.name.isNotBlank() }
                        .mapIndexed { index, field ->
                            field.copy(name = field.name.trim().ifBlank { "Поле ${index + 1}" })
                        }
                        .ifEmpty {
                            listOf(
                                EditableField(
                                    id = UUID.randomUUID().toString(),
                                    name = "Поле",
                                    fieldType = WidgetFieldType.TEXT
                                )
                            )
                        }

                    if (draft.widgetId == null) {
                        vm.addCustomWidget(
                            title = cleanedTitle,
                            fields = cleanedFields.mapIndexed { index, field ->
                                CustomWidgetField(
                                    id = field.id,
                                    name = field.name,
                                    fieldType = field.fieldType,
                                    unit = null,
                                    sortOrder = index
                                )
                            }
                        )
                    } else {
                        vm.updateWidget(
                            widgetId = draft.widgetId,
                            title = cleanedTitle,
                            fields = cleanedFields.mapIndexed { index, field ->
                                CustomWidgetField(
                                    id = field.id,
                                    name = field.name,
                                    fieldType = field.fieldType,
                                    unit = null,
                                    sortOrder = index
                                )
                            }
                        )
                    }
                    customDialog = null
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { customDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    editWidget?.let { widget ->
        var title by remember(widget.id) { mutableStateOf(widget.title) }
        AlertDialog(
            onDismissRequest = { editWidget = null },
            title = { Text("Редактировать услугу") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateWidget(widget.id, title.trim().ifBlank { widget.title }, null)
                    editWidget = null
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editWidget = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    deleteWidget?.let { widget ->
        AlertDialog(
            onDismissRequest = { deleteWidget = null },
            title = { Text("Удалить услугу?") },
            text = { Text("${widget.title} будет скрыта. Данные останутся в базе.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.archiveWidget(widget.id)
                    deleteWidget = null
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteWidget = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    entryWidget?.let { widget ->
        val widgetFields = fieldsByWidget[widget.id].orEmpty().sortedBy { it.sortOrder }
        var selectedYear by remember(widget.id) { mutableStateOf(now.year) }
        var selectedMonth by remember(widget.id) { mutableStateOf(now.monthValue) }
        val initialInputs = remember(widget.id, widgetFields, uiState.entries, selectedYear, selectedMonth) {
            buildInitialInputs(widgetFields, entriesByField, selectedYear, selectedMonth)
        }
        var inputs by remember(widget.id, selectedYear, selectedMonth) { mutableStateOf(initialInputs) }
        var imageTargetFieldId by remember(widget.id) { mutableStateOf<String?>(null) }
        val scrollState = rememberScrollState()
        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val targetId = imageTargetFieldId ?: return@rememberLauncherForActivityResult
            if (uri != null) {
                inputs = inputs.toMutableMap().also {
                    val state = it[targetId] ?: FieldInputState()
                    it[targetId] = state.copy(textValue = uri.toString())
                }
            }
        }

        AlertDialog(
            onDismissRequest = { entryWidget = null },
            title = { Text("Внести за ${monthName(selectedMonth)}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            val (y, m) = shiftMonth(selectedYear, selectedMonth, -1)
                            selectedYear = y
                            selectedMonth = m
                        }) {
                            Text("←")
                        }
                        Text("${monthName(selectedMonth)} $selectedYear")
                        TextButton(onClick = {
                            val (y, m) = shiftMonth(selectedYear, selectedMonth, 1)
                            selectedYear = y
                            selectedMonth = m
                        }) {
                            Text("→")
                        }
                    }
                    widgetFields.forEach { field ->
                        val state = inputs[field.id] ?: FieldInputState()
                        when (field.fieldType) {
                            WidgetFieldType.METER,
                            WidgetFieldType.MONEY -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = field.name,
                                        modifier = Modifier.weight(0.15f),
                                        fontWeight = FontWeight.Medium,
                                        color = waterColor(field.name)
                                    )
                                    OutlinedTextField(
                                        value = state.numberText,
                                        onValueChange = { value ->
                                            val filtered = filterDecimal(value)
                                            inputs = inputs.toMutableMap().also {
                                                it[field.id] = state.copy(numberText = filtered)
                                            }
                                        },
                                        placeholder = { Text("0.00 или 0,00") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.85f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                    )
                                }
                            }
                            WidgetFieldType.TEXT -> {
                                OutlinedTextField(
                                    value = state.textValue,
                                    onValueChange = { value ->
                                        inputs = inputs.toMutableMap().also {
                                            it[field.id] = state.copy(textValue = value)
                                        }
                                    },
                                    label = { Text(fieldLabel(field)) },
                                    singleLine = true
                                )
                            }
                            WidgetFieldType.STATUS -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(field.name)
                                        Text(if (state.statusPaid) "Оплачено" else "Не оплачено")
                                    }
                                    Switch(
                                        checked = state.statusPaid,
                                        onCheckedChange = { checked ->
                                            inputs = inputs.toMutableMap().also {
                                                it[field.id] = state.copy(statusPaid = checked)
                                            }
                                        }
                                    )
                                }
                            }
                            WidgetFieldType.IMAGE -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = field.name,
                                        modifier = Modifier.weight(0.4f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(
                                        modifier = Modifier.weight(0.6f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = {
                                            imageTargetFieldId = field.id
                                            imagePicker.launch("image/*")
                                        }) {
                                            Icon(Icons.Filled.AttachFile, contentDescription = "Прикрепить")
                                        }
                                        Text(if (state.textValue.isBlank()) "Файл не выбран" else "Выбрано")
                                    }
                                }
                                if (state.textValue.isNotBlank()) {
                                    AsyncImage(
                                        model = state.textValue,
                                        contentDescription = "Превью",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 220.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val entryInputs = widgetFields.mapNotNull { field ->
                        val state = inputs[field.id] ?: FieldInputState()
                        val numberValue = if (field.fieldType == WidgetFieldType.METER || field.fieldType == WidgetFieldType.MONEY) {
                            parseNumber(state.numberText)
                        } else {
                            null
                        }
                        val textValue = when (field.fieldType) {
                            WidgetFieldType.TEXT,
                            WidgetFieldType.IMAGE -> state.textValue.trim().ifBlank { null }
                            else -> null
                        }
                        val statusValue = if (field.fieldType == WidgetFieldType.STATUS) {
                            if (state.statusPaid) "PAID" else "UNPAID"
                        } else {
                            null
                        }

                        FieldEntryInput(
                            fieldId = field.id,
                            fieldType = field.fieldType,
                            valueNumber = numberValue,
                            valueText = textValue,
                            status = statusValue
                        )
                    }
                    vm.saveEntries(selectedYear, selectedMonth, entryInputs)
                    entryWidget = null
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryWidget = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    historyWidget?.let { widget ->
        val widgetFields = fieldsByWidget[widget.id].orEmpty().sortedBy { it.sortOrder }
        val historyItems = buildHistoryItems(widgetFields, entriesByField)

        AlertDialog(
            onDismissRequest = { historyWidget = null },
            title = { Text("История: ${widget.title}") },
            text = {
                if (historyItems.isEmpty()) {
                    Text("Записей пока нет")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyItems) { item ->
                            Column {
                                Text(
                                    text = "${monthName(item.month)} ${item.year}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                item.lines.forEach { line ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(line.name, color = waterColor(line.name))
                                        Text(line.value)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { historyWidget = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Показания") },
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Экран в процессе разработки",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Замечания и предложения: raid_hp_auto@mail.ru",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Поставщики услуг",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = { addDialogOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Добавить")
                }
            }

            if (uiState.widgets.isEmpty()) {
                Text("Пока нет услуг. Добавьте первую.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.widgets, key = { it.id }) { widget ->
                        val widgetFields = fieldsByWidget[widget.id].orEmpty().sortedBy { it.sortOrder }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(widget.title, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            if (widget.templateKey == WidgetTemplate.CUSTOM.key) {
                                                customDialog = CustomWidgetDraft(
                                                    widgetId = widget.id,
                                                    title = widget.title,
                                                    fields = widgetFields.map {
                                                        EditableField(
                                                            id = it.id,
                                                            name = it.name,
                                                            fieldType = it.fieldType
                                                        )
                                                    }
                                                )
                                            } else {
                                                editWidget = widget
                                            }
                                        }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                                        }
                                        IconButton(onClick = { deleteWidget = widget }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                                        }
                                    }
                                }

                                widgetFields.forEach { field ->
                                    val fieldEntries = entriesByField[field.id].orEmpty()
                                    val currentEntry = fieldEntries.firstOrNull {
                                        it.periodYear == now.year && it.periodMonth == now.monthValue
                                    }
                                    val prevEntry = findPreviousEntry(fieldEntries, now.year, now.monthValue)

                                    val currentText = formatFieldValue(field, currentEntry)
                                    val prevText = formatFieldValue(field, prevEntry)

                                    val deltaText = if (field.fieldType == WidgetFieldType.METER) {
                                        val currentVal = currentEntry?.valueNumber
                                        val prevVal = prevEntry?.valueNumber
                                        if (currentVal != null && prevVal != null) {
                                            val delta = currentVal - prevVal
                                            "Δ ${formatNumber(delta)}"
                                        } else {
                                            "Δ —"
                                        }
                                    } else {
                                        null
                                    }

                                    Column {
                                        Text(field.name, fontWeight = FontWeight.SemiBold, color = waterColor(field.name))
                                        Text("Текущее: $currentText")
                                        Text("Прошлое: $prevText")
                                        if (deltaText != null) {
                                            Text(deltaText)
                                        }
                                        if (field.fieldType == WidgetFieldType.IMAGE && !currentEntry?.valueText.isNullOrBlank()) {
                                            AsyncImage(
                                                model = currentEntry?.valueText,
                                                contentDescription = "Изображение",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 180.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ElevatedButton(onClick = { entryWidget = widget }) {
                                        Text("Внести за месяц")
                                    }
                                    TextButton(onClick = { historyWidget = widget }) {
                                        Text("История")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class HistoryLine(
    val name: String,
    val value: String
)

private data class HistoryItem(
    val year: Int,
    val month: Int,
    val lines: List<HistoryLine>
)

private fun fieldLabel(field: WidgetField): String {
    val unit = field.unit?.let { " ($it)" } ?: ""
    return "${field.name}$unit"
}

private fun parseNumber(text: String): Double? {
    val cleaned = text.trim().replace(',', '.')
    return cleaned.toDoubleOrNull()
}

private fun formatFieldValue(field: WidgetField, entry: FieldEntry?): String {
    if (entry == null) return "—"

    return when (field.fieldType) {
        WidgetFieldType.METER -> entry.valueNumber?.let { "${formatNumber(it)}${field.unit?.let { unit -> " $unit" } ?: ""}" } ?: "—"
        WidgetFieldType.MONEY -> entry.valueNumber?.let { "${moneyFormatPlain(it)}${field.unit?.let { unit -> " $unit" } ?: ""}" } ?: "—"
        WidgetFieldType.TEXT -> entry.valueText ?: "—"
        WidgetFieldType.STATUS -> when (entry.status) {
            "PAID" -> "Оплачено"
            "UNPAID" -> "Не оплачено"
            else -> "—"
        }
        WidgetFieldType.IMAGE -> if (entry.valueText.isNullOrBlank()) "—" else "Есть"
    }
}

private fun findPreviousEntry(entries: List<FieldEntry>, year: Int, month: Int): FieldEntry? {
    val currentKey = year * 100 + month
    return entries
        .filter { it.periodYear * 100 + it.periodMonth < currentKey }
        .maxByOrNull { it.periodYear * 100 + it.periodMonth }
}

private fun formatNumber(value: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return nf.format(value)
}

private fun buildInitialInputs(
    fields: List<WidgetField>,
    entriesByField: Map<String, List<FieldEntry>>,
    year: Int,
    month: Int
): Map<String, FieldInputState> {
    return fields.associate { field ->
        val entry = entriesByField[field.id]
            ?.firstOrNull { it.periodYear == year && it.periodMonth == month }
        val state = when (field.fieldType) {
            WidgetFieldType.METER,
            WidgetFieldType.MONEY -> {
                FieldInputState(numberText = entry?.valueNumber?.toString() ?: "")
            }
            WidgetFieldType.TEXT,
            WidgetFieldType.IMAGE -> {
                FieldInputState(textValue = entry?.valueText ?: "")
            }
            WidgetFieldType.STATUS -> {
                FieldInputState(statusPaid = entry?.status == "PAID")
            }
        }
        field.id to state
    }
}

private fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val total = year * 12 + (month - 1) + delta
    val newYear = total / 12
    val newMonth = (total % 12) + 1
    return newYear to newMonth
}

private fun buildHistoryItems(
    fields: List<WidgetField>,
    entriesByField: Map<String, List<FieldEntry>>
): List<HistoryItem> {
    val allEntries = fields.flatMap { field ->
        entriesByField[field.id].orEmpty().map { it to field }
    }

    val grouped = allEntries.groupBy { it.first.periodYear to it.first.periodMonth }

    return grouped.entries
        .sortedByDescending { (key, _) -> key.first * 100 + key.second }
        .map { (key, items) ->
            val (year, month) = key
            val lines = items.sortedBy { it.second.sortOrder }.map { (entry, field) ->
                val value = formatFieldValue(field, entry)
                HistoryLine(name = field.name, value = value)
            }
            HistoryItem(year = year, month = month, lines = lines)
        }
}

private fun fieldTypeLabel(type: WidgetFieldType): String = when (type) {
    WidgetFieldType.TEXT -> "Текст"
    WidgetFieldType.METER -> "Счётчик"
    WidgetFieldType.MONEY -> "Деньги"
    WidgetFieldType.STATUS -> "Статус"
    WidgetFieldType.IMAGE -> "Изображение"
}

private fun fieldTypeOptions(): List<WidgetFieldType> = listOf(
    WidgetFieldType.TEXT,
    WidgetFieldType.METER,
    WidgetFieldType.MONEY,
    WidgetFieldType.STATUS,
    WidgetFieldType.IMAGE
)

private fun filterDecimal(value: String): String =
    value.filter { it.isDigit() || it == '.' || it == ',' }

@Composable
private fun waterColor(name: String): Color = when {
    name.contains("ХВС", ignoreCase = true) -> Color(0xFF1E88E5)
    name.contains("ГВС", ignoreCase = true) -> Color(0xFFD32F2F)
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun TemplateOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
