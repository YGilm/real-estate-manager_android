@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.components.EditTransactionDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun PropertyDetailsScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onEditProperty: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var property by remember { mutableStateOf<Property?>(null) }
    var txs by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var txToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // первая загрузка данных
    LaunchedEffect(propertyId) {
        property = vm.getProperty(propertyId)
        txs = vm.transactionsFor(propertyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(property?.name ?: "Объект") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditProperty(propertyId) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать объект")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить транзакцию")
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val p = property
            if (p != null) {
                Text("Адрес: ${p.address ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Аренда/мес: ${p.monthlyRent?.toString() ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("Транзакции", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()

            if (txs.isEmpty()) {
                Text("Пока нет транзакций")
            } else {
                txs.forEach { tx ->
                    TransactionRow(
                        tx = tx,
                        onEdit = { txToEdit = tx },
                        onDelete = {
                            scope.launch {
                                vm.deleteTransaction(tx.id)
                                txs = vm.transactionsFor(propertyId)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    // Диалог редактирования
    txToEdit?.let { t ->
        EditTransactionDialog(
            initial = t,
            onSave = { isIncome, amount, date, note ->
                scope.launch {
                    vm.updateTransaction(
                        id = t.id,
                        isIncome = isIncome,
                        amount = amount,
                        date = date,
                        note = note
                    )
                    txs = vm.transactionsFor(propertyId)
                }
                txToEdit = null
            },
            onDelete = {
                scope.launch {
                    vm.deleteTransaction(t.id)
                    txs = vm.transactionsFor(propertyId)
                }
                txToEdit = null
            },
            onDismiss = { txToEdit = null }
        )
    }

    // Диалог добавления
    if (showAddDialog) {
        AddTransactionDialog(
            onSave = { isIncome, amount, date, note ->
                scope.launch {
                    vm.addTransaction(
                        propertyId = propertyId,
                        isIncome = isIncome,
                        amount = amount,
                        date = date,
                        note = note
                    )
                    txs = vm.transactionsFor(propertyId)
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            val typeText = if (tx.type == TxType.INCOME) "Доход" else "Расход"
            Text("$typeText — ${tx.amount}")
            Text(tx.date.toString(), style = MaterialTheme.typography.bodySmall)
            if (!tx.note.isNullOrBlank()) {
                Text(tx.note!!, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
    }
}

/** Диалог добавления транзакции (локально в этом файле) */
@Composable
private fun AddTransactionDialog(
    onSave: (isIncome: Boolean, amount: Double, date: LocalDate, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var isIncome by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val todayMillis = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.replace(",", ".").toDoubleOrNull() ?: return@TextButton
                val selectedMillis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                val localDate = Instant.ofEpochMilli(selectedMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                onSave(isIncome, amount, localDate, note.ifBlank { null })
            }) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
        title = { Text("Новая транзакция") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Доход") })
                    FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Расход") })
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                DatePicker(state = dateState)
            }
        }
    )
}