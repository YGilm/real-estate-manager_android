@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
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
    onSave: (
        Boolean,
        Double,
        LocalDate,
        String?,
        String?,
        String?,
        String?
    ) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var isIncome by remember { mutableStateOf(initial.type == TxType.INCOME) }

    var amountText by remember {
        mutableStateOf(if (initial.amount == 0.0) "" else formatAmountForEdit(initial.amount))
    }

    var note by remember { mutableStateOf(initial.note ?: "") }

    // ---- ВЛОЖЕНИЕ ----
    var attachmentUri by remember { mutableStateOf(initial.attachmentUri) }
    var attachmentMime by remember { mutableStateOf(initial.attachmentMime) }
    var attachmentNameEditable by remember { mutableStateOf(initial.attachmentName ?: "") }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        attachmentUri = uri.toString()
        attachmentMime = context.contentResolver.getType(uri)

        val pickedName = queryDisplayName(context, uri) ?: (uri.lastPathSegment ?: "")
        attachmentNameEditable = pickedName
    }

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

    // ✅ делаем поле кликабельным, но визуально “как обычное”
    val dateInteraction = remember { MutableInteractionSource() }
    val datePressed by dateInteraction.collectIsPressedAsState()
    LaunchedEffect(datePressed) {
        if (datePressed) dateDialogOpen = true
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

                    val amount = normalized.toDoubleOrNull() ?: return@TextButton
                    val finalName = attachmentNameEditable.trim().ifBlank { null }

                    onSave(
                        isIncome,
                        amount,
                        date,
                        note.ifBlank { null },
                        attachmentUri,
                        finalName,
                        attachmentMime
                    )
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Удалить") }
            }
        },
        title = {
            Box(
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ✅ дата теперь не серая: enabled=true, readOnly=true
                OutlinedTextField(
                    value = date.format(dateLabelFmt),
                    onValueChange = {},
                    label = { Text("Дата") },
                    readOnly = true,
                    enabled = true,
                    interactionSource = dateInteraction,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Вложение (счёт/чек)",
                            style = MaterialTheme.typography.labelLarge
                        )

                        if (attachmentUri.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = { pickFileLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.AttachFile, contentDescription = "Прикрепить файл")
                                Spacer(Modifier.width(8.dp))
                                Text("Прикрепить файл")
                            }
                        } else {
                            OutlinedTextField(
                                value = attachmentNameEditable,
                                onValueChange = { attachmentNameEditable = it },
                                singleLine = true,
                                label = { Text("Название файла") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val uri = runCatching { Uri.parse(attachmentUri) }.getOrNull()
                                        if (uri != null) openAttachment(context, uri, attachmentMime)
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Открыть файл")
                                }

                                Spacer(Modifier.width(14.dp))

                                FilledTonalIconButton(
                                    onClick = { pickFileLauncher.launch(arrayOf("*/*")) }
                                ) {
                                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Заменить файл")
                                }

                                Spacer(Modifier.width(14.dp))

                                FilledTonalIconButton(
                                    onClick = {
                                        attachmentUri = null
                                        attachmentMime = null
                                        attachmentNameEditable = ""
                                    }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Удалить файл")
                                }
                            }
                        }
                    }
                }

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
                ) { Text("Ок") }
            },
            dismissButton = { TextButton(onClick = { dateDialogOpen = false }) { Text("Отмена") } }
        ) { DatePicker(state = dateState) }
    }
}

private fun formatAmountForEdit(value: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        isGroupingUsed = false
    }
    val raw = nf.format(value)
    return raw.replace('.', ',')
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    return runCatching {
        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }.getOrNull()
}

private fun openAttachment(context: Context, uri: Uri, mime: String?) {
    val type = mime ?: context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, type)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Открыть файл"))
    }.onFailure {
        Toast.makeText(context, "Не удалось открыть файл: ${it.message}", Toast.LENGTH_LONG).show()
    }
}
