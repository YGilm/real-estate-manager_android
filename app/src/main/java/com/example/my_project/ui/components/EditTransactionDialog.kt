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

    // ⬇ сумма: если 0.0 — поле пустое, иначе форматируем
    var amountText by remember {
        mutableStateOf(
            if (initial.amount == 0.0) "" else formatMoneyForField(initial.amount)
        )
    }

    var note by remember { mutableStateOf(initial.note ?: "") }

    // ---- ДАТА (оставляем логику как в рабочей версии) ----
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
                val normalized = amountText
                    .replace(" ", "")
                    .replace("\u00A0", "") // если будут неразрывные пробелы
                    .replace(",", ".")
                    .trim()
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
        title = {
            Text("Редактировать транзакцию")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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

                // ---- СУММА: автоформат, пробелы, 2 знака ----
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        val cleaned = newValue
                            .filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }

                        if (cleaned.isBlank()) {
                            amountText = ""
                        } else {
                            val normalized = cleaned
                                .replace(" ", "")
                                .replace("\u00A0", "")
                                .replace(",", ".")
                            val parsed = normalized.toDoubleOrNull()
                            amountText = if (parsed != null) {
                                formatMoneyForField(parsed)
                            } else {
                                cleaned
                            }
                        }
                    },
                    label = { Text("Сумма") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // ---- ДАТА: как было, через Box + disabled поле ----
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dateDialogOpen = true }
                ) {
                    OutlinedTextField(
                        value = date.format(dateLabelFmt),
                        onValueChange = {},
                        label = { Text("Дата") },
                        readOnly = true,
                        enabled = false,   // да, оно серое, зато 100% работает
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                    date = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
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
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
        isGroupingUsed = true   // пробелы между тысячами
    }
    return nf.format(value)
}