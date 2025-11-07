@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.util.Totals
import com.example.my_project.ui.util.computeTotals
import com.example.my_project.ui.util.moneyFormat
import com.example.my_project.ui.util.moneyFormatPlain
import com.example.my_project.ui.util.monthName
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs

/* ---------- локальное состояние диалогов ---------- */
private sealed interface EditState {
    /** null = сначала спросим тип (Доход/Расход) */
    data class Add(val isIncome: Boolean?) : EditState
    data class Edit(val tx: Transaction) : EditState
}

@Composable
fun PropertyDetailsScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onEditProperty: (String) -> Unit,
    onOpenStatsForProperty: (String) -> Unit = {}
) {
    val properties by vm.properties.collectAsState()
    val allTx by vm.transactions.collectAsState()

    val property = properties.firstOrNull { it.id == propertyId }

    // фильтр периода (одна строка: Год + Месяц)
    val now = remember { LocalDate.now() }
    val availableYears = remember(allTx, propertyId) {
        val ys = allTx.filter { it.propertyId == propertyId }.map { it.date.year }.distinct()
        if (ys.isEmpty()) listOf(now.year) else ys.sortedDescending()
    }
    var year by rememberSaveable(availableYears) { mutableStateOf(availableYears.first()) }
    var month by rememberSaveable { mutableStateOf(now.monthValue) }

    val periodTx = remember(allTx, propertyId, year, month) {
        allTx.filter {
            it.propertyId == propertyId && it.date.year == year && it.date.monthValue == month
        }.sortedByDescending { it.date }
    }
    val totals = remember(periodTx) { periodTx.computeTotals() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    var editState by remember { mutableStateOf<EditState?>(null) }
    var deleteTarget by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Одной строкой и без переполнения
                    Text(
                        text = property?.name ?: "Объект",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditProperty(propertyId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать объект")
                    }
                    IconButton(onClick = { onOpenStatsForProperty(propertyId) }) {
                        Icon(Icons.Filled.BarChart, contentDescription = "Статистика по объекту")
                    }
                }
            )
        },
        floatingActionButton = {
            // Один плюс: сначала спросим тип, затем откроем форму
            FloatingActionButton(onClick = { editState = EditState.Add(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить транзакцию")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "${monthName(month)} $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            // одна строка: Год + Месяц
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                YearPicker(
                    value = year,
                    options = availableYears,
                    onSelect = { year = it },
                    modifier = Modifier.weight(1f)
                )
                MonthPicker(
                    value = month,
                    onSelect = { month = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            PropertyTotalsBlock(totals)

            Spacer(Modifier.height(16.dp))
            Text("Транзакции", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (periodTx.isEmpty()) {
                Text("Нет транзакций за выбранный период", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periodTx) { t ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (t.type == TxType.INCOME) "Доход" else "Расход",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row {
                                        IconButton(onClick = { editState = EditState.Edit(t) }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                                        }
                                        IconButton(onClick = { deleteTarget = t }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                                        }
                                    }
                                }
                                MoneyLine(type = t.type, amount = t.amount)
                                Text(t.date.format(dateFmt), style = MaterialTheme.typography.bodySmall)
                                if (!t.note.isNullOrBlank()) {
                                    Text(t.note!!, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /* ---------- Диалоги ---------- */
    when (val st = editState) {
        is EditState.Add -> {
            if (st.isIncome == null) {
                // Сначала выбираем тип
                val primary = MaterialTheme.colorScheme.primary
                AlertDialog(
                    onDismissRequest = { editState = null },
                    title = { Text("Добавить транзакцию") },
                    text = {
                        Text("Выберите тип транзакции:")
                    },
                    confirmButton = {
                        TextButton(onClick = { editState = EditState.Add(true) }) {
                            Text("Доход", color = primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editState = EditState.Add(false) }) {
                            Text("Расход", color = primary)
                        }
                    }
                )
            } else {
                AddEditTxDialog(
                    title = if (st.isIncome) "Добавить доход" else "Добавить расход",
                    initIsIncome = st.isIncome,
                    onDismiss = { editState = null },
                    onSave = { isIncome, amount, date, note ->
                        vm.addTransaction(propertyId, isIncome, amount, date, note)
                        editState = null
                    }
                )
            }
        }

        is EditState.Edit -> {
            AddEditTxDialog(
                title = if (st.tx.type == TxType.INCOME) "Редактировать доход" else "Редактировать расход",
                initIsIncome = st.tx.type == TxType.INCOME,
                initAmount = st.tx.amount,
                initDate = st.tx.date,
                initNote = st.tx.note ?: "",
                onDismiss = { editState = null },
                onSave = { isIncome, amount, date, note ->
                    vm.updateTransaction(
                        id = st.tx.id,
                        isIncome = isIncome,
                        amount = amount,
                        date = date,
                        note = note
                    )
                    editState = null
                }
            )
        }

        null -> Unit
    }

    // Подтверждение удаления
    deleteTarget?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Удалить транзакцию?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTransaction(toDelete.id)
                    deleteTarget = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
            }
        )
    }
}

/* ---------- Пикеры: Год и Месяц (в одну строку) ---------- */

@Composable
private fun YearPicker(
    value: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value.toString(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Год") },
            trailingIcon = {
                val icon = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
                Icon(
                    imageVector = icon,
                    contentDescription = "Раскрыть",
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .clickable { expanded = true }
                .fillMaxWidth()
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { y ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(y.toString()) },
                    onClick = {
                        onSelect(y)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MonthPicker(
    value: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val months = remember { (1..12).map { it to monthName(it) } }
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = months.first { it.first == value }.second,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Месяц") },
            trailingIcon = {
                val icon = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
                Icon(
                    imageVector = icon,
                    contentDescription = "Раскрыть",
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .clickable { expanded = true }
                .fillMaxWidth()
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEach { (m, title) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelect(m)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* ---------- Блок сумм для объекта ---------- */

@Composable
private fun PropertyTotalsBlock(t: Totals) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MoneyLineLabel("Доход:", TxType.INCOME, t.income)
        MoneyLineLabel("Расход:", TxType.EXPENSE, t.expense)
        MoneyLineTotal("Итого:", t.income - t.expense)
    }
}

@Composable
private fun MoneyLine(type: TxType, amount: Double) {
    val text = moneyFormat(amount, type)
    val color = if (type == TxType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
    Text(text, color = color, fontWeight = FontWeight.Medium)
}

@Composable
private fun MoneyLineLabel(label: String, type: TxType, amount: Double) {
    val text = moneyFormat(amount, type)
    val color = if (type == TxType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(text, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MoneyLineTotal(label: String, amount: Double) {
    val text = moneyFormatPlain(amount)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/* ---------- Диалог добавления/редактирования ---------- */

@Composable
private fun AddEditTxDialog(
    title: String,
    initIsIncome: Boolean,
    initAmount: Double? = null,
    initDate: LocalDate? = null,
    initNote: String = "",
    onDismiss: () -> Unit,
    onSave: (isIncome: Boolean, amount: Double, date: LocalDate, note: String?) -> Unit
) {
    var isIncome by rememberSaveable { mutableStateOf(initIsIncome) }
    var amountText by rememberSaveable { mutableStateOf(initAmount?.let { formatPlain(it) } ?: "") }
    var dateText by rememberSaveable {
        val d = initDate ?: LocalDate.now()
        mutableStateOf(d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
    }
    var note by rememberSaveable { mutableStateOf(initNote) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val highlight = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Тип:")
                    Text(
                        "Доход",
                        modifier = Modifier
                            .border(1.dp, if (isIncome) highlight else Color.Gray, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { isIncome = true },
                        color = if (isIncome) highlight else Color.Unspecified
                    )
                    Text(
                        "Расход",
                        modifier = Modifier
                            .border(1.dp, if (!isIncome) highlight else Color.Gray, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable { isIncome = false },
                        color = if (!isIncome) highlight else Color.Unspecified
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    singleLine = true,
                    label = { Text("Сумма") }
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    singleLine = true,
                    label = { Text("Дата (дд.ММ.гггг)") }
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Комментарий") }
                )
                if (error != null) Text(error!!, color = Color(0xFFC62828))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.replace(',', '.').toDoubleOrNull()
                if (amount == null) {
                    error = "Некорректная сумма"
                    return@TextButton
                }
                val date = try {
                    LocalDate.parse(dateText, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                } catch (_: DateTimeParseException) {
                    error = "Некорректная дата"
                    return@TextButton
                }
                onSave(isIncome, amount, date, note.ifBlank { null })
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

/* Вспомогательное форматирование для стартового значения суммы */
private fun formatPlain(v: Double): String {
    val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return nf.format(abs(v))
}