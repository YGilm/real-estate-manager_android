@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val propertyTransactions = transactions.filter { it.propertyId == propertyId }

    var currentEditing by remember(propertyId, propertyTransactions.size) { mutableStateOf<Transaction?>(null) }
    var isNew by remember { mutableStateOf(false) }

    // форматтер для заголовков месяцев (считается один раз на композицию)
    val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru", "RU"))

    // группируем по YearMonth и сортируем по убыванию (свежие месяцы сверху)
    val groupedByMonth: Map<YearMonth, List<Transaction>> =
        propertyTransactions
            .sortedByDescending { it.date }
            .groupBy { YearMonth.from(it.date) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Транзакции") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
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
                        note = null
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Добавить транзакцию"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (propertyTransactions.isEmpty()) {
                Text(
                    text = "На данный момент транзакций нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        isNew = true
                        currentEditing = Transaction(
                            propertyId = propertyId,
                            type = TxType.INCOME,
                            amount = 0.0,
                            date = LocalDate.now(),
                            note = null
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Добавить"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Добавить транзакцию")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedByMonth.forEach { (yearMonth, list) ->
                        val headerText = yearMonth
                            .atDay(1)
                            .format(monthFormatter)
                            .replaceFirstChar { ch ->
                                if (ch.isLowerCase()) ch.titlecase(Locale("ru", "RU")) else ch.toString()
                            }

                        // заголовок месяца
                        item(key = "header_${yearMonth}") {
                            Text(
                                text = headerText,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        // элементы месяца
                        items(list, key = { it.id }) { tx ->
                            TransactionRow(
                                transaction = tx,
                                onClick = {
                                    isNew = false
                                    currentEditing = tx
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (currentEditing != null) {
        EditTransactionDialog(
            initial = currentEditing!!,
            isNew = isNew,
            onSave = { isIncome, amount, date, note ->
                if (isNew) {
                    vm.addTransaction(
                        propertyId = propertyId,
                        isIncome = isIncome,
                        amount = amount,
                        date = date,
                        note = note
                    )
                } else {
                    vm.updateTransaction(
                        id = currentEditing!!.id,
                        isIncome = isIncome,
                        amount = amount,
                        date = date,
                        note = note
                    )
                }
                currentEditing = null
            },
            onDelete = {
                if (!isNew) {
                    vm.deleteTransaction(currentEditing!!.id)
                }
                currentEditing = null
            },
            onDismiss = { currentEditing = null }
        )
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

    // Сегодня и флаг "будущая транзакция"
    val today = LocalDate.now()
    val isFuture = transaction.date.isAfter(today)

    // Базовые цвета для дохода / расхода
    val baseAmountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)

    // Если транзакция в будущем — всё делаем серым/приглушённым
    val amountColor = if (isFuture) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        baseAmountColor
    }

    val dateColor = if (isFuture) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val noteColor = if (isFuture) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val sign = if (isIncome) "+" else "-"
    val amountText = remember(transaction.amount, transaction.type) {
        sign + String.format(Locale("ru", "RU"), "%.2f", transaction.amount)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
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
                    color = dateColor
                )
            }
            if (!transaction.note.isNullOrBlank()) {
                Text(
                    text = transaction.note!!,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = noteColor
                )
            }
        }
    }
}