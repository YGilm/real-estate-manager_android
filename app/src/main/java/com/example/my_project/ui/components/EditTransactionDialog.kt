@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.style.TextOverflow
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
    isNew: Boolean,
    onSave: (isIncome: Boolean, amount: Double, date: LocalDate, note: String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var isIncome by remember { mutableStateOf(initial.type == TxType.INCOME) }

    // Сумма как "сырая" строка, без автоформатирования при вводе
    var amountText by remember {
        mutableStateOf(
            if (initial.amount == 0.0) "" else formatAmountForEdit(initial.amount)
        )
    }

    var note by remember { mutableStateOf(initial.note ?: "") }

    // ---- ДАТА ----
    var date by remember { mutableStateOf(initial.date) }
    val dateLabelFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    var dateDialogOpen by remember { mutableStateOf(false) }

    val initialMillis = remember(initial.date) {
        initial.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    LaunchedEffect(dateDialogOpen) {
        if (dateDialogOpen) {
            dateState.selectedDateMillis = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = amountText
                        .replace(" ", "")
                        .replace("\u00A0", "")
                        .replace(",", ".")
                        .trim()

                    val amount = normalized.toDoubleOrNull()
                    if (amount == null) {
                        // Можно позже добавить подсветку ошибки, пока просто не закрываем
                        return@TextButton
                    }

                    onSave(
                        isIncome,
                        amount,
                        date,
                        note.ifBlank { null }
                    )
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
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
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = if (isNew) "Добавление транзакции" else "Редактирование транзакции",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---- ТИП: ДОХОД / РАСХОД В ОДНУ СТРОКУ, СИММЕТРИЧНО ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = {
                            Box(Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Доход",
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = {
                            Box(Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Расход",
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ---- СУММА ----
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        var separatorSeen = false
                        val filtered = buildString {
                            for (ch in newValue) {
                                when {
                                    ch.isDigit() -> append(ch)
                                    (ch == ',' || ch == '.') && !separatorSeen -> {
                                        append(ch)
                                        separatorSeen = true
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        amountText = filtered
                    },
                    label = { Text("Сумма") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // ---- ДАТА ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dateDialogOpen = true }
                ) {
                    OutlinedTextField(
                        value = date.format(dateLabelFmt),
                        onValueChange = {},
                        label = { Text("Дата") },
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---- КОММЕНТАРИЙ ----
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
                TextButton(
                    onClick = {
                        val millis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        date = selectedDate
                        dateDialogOpen = false
                    }
                ) {
                    Text("Ок")
                }
            },
            dismissButton = {
                TextButton(onClick = { dateDialogOpen = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

/**
 * Преобразуем Double в удобную строку для редактирования:
 * - без разделителей тысяч
 * - максимум 2 знака после запятой
 * - разделитель десятых — запятая (под RU-привычку)
 */
private fun formatAmountForEdit(value: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        isGroupingUsed = false
    }
    val raw = nf.format(value) // "1234.5" или "1234.56"
    return raw.replace('.', ',')
}