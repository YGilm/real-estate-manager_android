@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EditTransactionDialog(
    initial: Transaction,
    onSave: (isIncome: Boolean, amount: Double, date: LocalDate, note: String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var isIncome by remember { mutableStateOf(initial.type == TxType.INCOME) }
    var amountText by remember { mutableStateOf(formatMoneyForField(initial.amount)) }
    var note by remember { mutableStateOf(initial.note ?: "") }

    // состояние даты
    var date by remember { mutableStateOf(initial.date) }
    val dateLabelFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    var dateDialogOpen by remember { mutableStateOf(false) }

    val initialMillis = remember(initial.date) {
        initial.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    // Синхронизируем selectedDateMillis при открытии диалога
    LaunchedEffect(dateDialogOpen) {
        if (dateDialogOpen) {
            dateState.selectedDateMillis = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val normalized = amountText.replace(",", ".").trim()
                val amount = normalized.toDoubleOrNull() ?: return@TextButton
                onSave(isIncome, amount, date, note.ifBlank { null })
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            }
        },
        title = { Text("Редактировать транзакцию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Тип
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Доход") }
                    )
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Расход") }
                    )
                }

                // Сумма
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Дата — кликабельное поле, открывающее DatePickerDialog
                OutlinedTextField(
                    value = date.format(dateLabelFmt),
                    onValueChange = {},
                    label = { Text("Дата") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dateDialogOpen = true }
                )

                // Комментарий
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))
            }
        }
    )

    if (dateDialogOpen) {
        DatePickerDialog(
            onDismissRequest = { dateDialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                    date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    dateDialogOpen = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { dateDialogOpen = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

private fun formatMoneyForField(value: Double): String {
    // для поля ввода оставим простой вид (точка/запятая допустимы)
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return nf.format(value)
}