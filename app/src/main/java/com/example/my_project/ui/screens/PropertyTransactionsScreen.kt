@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.components.EditTransactionDialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PropertyTransactionsScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
) {
    val transactions by vm.transactions.collectAsState()
    val propertyTransactions = remember(transactions, propertyId) {
        transactions.filter { it.propertyId == propertyId }
    }

    var currentDetails by remember(propertyId) { mutableStateOf<Transaction?>(null) }
    var currentEditing by remember(propertyId) { mutableStateOf<Transaction?>(null) }
    var isNew by remember(propertyId) { mutableStateOf(false) }

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru", "RU"))
    }

    val groupedByMonth = remember(propertyTransactions) {
        propertyTransactions
            .sortedByDescending { it.date }
            .groupBy { YearMonth.from(it.date) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Транзакции") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isNew = true
                    currentEditing = Transaction(
                        propertyId = propertyId,
                        type = TxType.INCOME,
                        amount = 0.0,
                        date = LocalDate.now(),
                        note = null,
                        attachmentUri = null,
                        attachmentName = null,
                        attachmentMime = null
                    )
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить транзакцию")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (propertyTransactions.isEmpty()) {
                Text(
                    text = "На данный момент транзакций нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedByMonth.forEach { (yearMonth, list) ->
                        val headerText = yearMonth.atDay(1).format(monthFormatter)
                            .replaceFirstChar { ch ->
                                if (ch.isLowerCase()) ch.titlecase(Locale("ru", "RU")) else ch.toString()
                            }

                        item(key = "header_$yearMonth") {
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(list, key = { it.id }) { tx ->
                            TransactionRow(transaction = tx, onClick = { currentDetails = tx })
                        }
                    }
                }
            }
        }
    }

    val detailsTx = currentDetails
    if (detailsTx != null) {
        TransactionDetailsSheet(
            transaction = detailsTx,
            onDismiss = { currentDetails = null },
            onEdit = {
                isNew = false
                currentEditing = detailsTx
                currentDetails = null
            }
        )
    }

    val editingTx = currentEditing
    if (editingTx != null) {
        EditTransactionDialog(
            initial = editingTx,
            isNew = isNew,
            onSave = { isIncome, amount, date, note, attachmentUri, attachmentName, attachmentMime ->
                if (isNew) {
                    vm.addTransaction(
                        propertyId = propertyId,
                        isIncome = isIncome,
                        amount = amount,
                        date = date,
                        note = note,
                        attachmentUri = attachmentUri,
                        attachmentName = attachmentName,
                        attachmentMime = attachmentMime
                    )
                } else {
                    vm.updateTransaction(
                        id = editingTx.id,
                        isIncome = isIncome,
                        amount = amount,
                        date = date,
                        note = note,
                        attachmentUri = attachmentUri,
                        attachmentName = attachmentName,
                        attachmentMime = attachmentMime
                    )
                }
                currentEditing = null
            },
            onDelete = {
                if (!isNew) vm.deleteTransaction(editingTx.id)
                currentEditing = null
            },
            onDismiss = { currentEditing = null }
        )
    }
}

@Composable
private fun TransactionDetailsSheet(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    val isIncome = transaction.type == TxType.INCOME
    val sign = if (isIncome) "+" else "-"
    val amountText = remember(transaction.amount, transaction.type) {
        sign + String.format(Locale("ru", "RU"), "%.2f", transaction.amount)
    }

    val baseAmountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val amountColor =
        if (transaction.date.isAfter(LocalDate.now())) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        else baseAmountColor

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Транзакция",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = amountText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = amountColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = transaction.date.format(formatter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = if (isIncome) "Доход" else "Расход",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!transaction.note.isNullOrBlank()) {
                        HorizontalDivider(Modifier.padding(top = 6.dp))
                        Text(text = transaction.note!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (!transaction.attachmentUri.isNullOrBlank()) {
                val shownName = transaction.attachmentName ?: "Вложение"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = shownName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // ✅ по центру, умеренная ширина
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val uri = runCatching { Uri.parse(transaction.attachmentUri) }.getOrNull()
                                    if (uri != null) openAttachment(context, uri, transaction.attachmentMime)
                                },
                                modifier = Modifier.width(220.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Открыть файл")
                            }
                        }
                    }
                }
            }

            // ✅ по центру, не на всю ширину
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier.width(240.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Редактировать", maxLines = 1)
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Закрыть")
            }

            Spacer(Modifier.height(6.dp))
        }
    }
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

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    val isIncome = transaction.type == TxType.INCOME
    val isFuture = transaction.date.isAfter(LocalDate.now())

    val baseAmountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val amountColor =
        if (isFuture) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else baseAmountColor

    val sign = if (isIncome) "+" else "-"
    val amountText = remember(transaction.amount, transaction.type) {
        sign + String.format(Locale("ru", "RU"), "%.2f", transaction.amount)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.date.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!transaction.note.isNullOrBlank()) {
                Text(
                    text = transaction.note!!,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
