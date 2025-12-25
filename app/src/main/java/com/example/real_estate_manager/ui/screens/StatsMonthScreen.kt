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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.real_estate_manager.data.model.Transaction
import com.example.real_estate_manager.data.model.TxType
import com.example.real_estate_manager.ui.RealEstateViewModel
import com.example.real_estate_manager.ui.util.DateFmtDMY
import com.example.real_estate_manager.ui.util.Totals
import com.example.real_estate_manager.ui.util.computeTotals
import com.example.real_estate_manager.ui.util.moneyFormatPlain
import com.example.real_estate_manager.ui.util.monthName
import java.time.LocalDate

@Composable
fun StatsMonthScreen(
    vm: RealEstateViewModel,
    year: Int,
    month: Int,
    propertyId: String?,
    onBack: () -> Unit
) {
    val allTx by vm.transactions.collectAsState()
    val txs = allTx.filter { t ->
        t.date.year == year && t.date.monthValue == month &&
                (propertyId == null || t.propertyId == propertyId)
    }.sortedByDescending { it.date }

    // computeTotals() уже отбрасывает будущие транзакции
    val totals = txs.computeTotals()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика: ${monthName(month)} $year") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
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
                .padding(16.dp)
                .padding(bottom = 16.dp)
        ) {
            TotalsRow(totals)
            Spacer(Modifier.height(16.dp))

            Text("Транзакции", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(txs) { t -> TxCard(t) }
            }
        }
    }
}

@Composable
private fun TotalsRow(t: Totals) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MoneyLineLabel("Доход:", TxType.INCOME, t.income)
            MoneyLineLabel("Расход:", TxType.EXPENSE, t.expense)
            MoneyLineTotal("Итого:", t.total)
        }
    }
}

@Composable
private fun TxCard(t: Transaction) {
    val today = LocalDate.now()
    val isFuture = t.date.isAfter(today)

    val headerColor = if (isFuture) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurface
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = if (t.type == TxType.INCOME) "Доход" else "Расход",
                fontWeight = FontWeight.SemiBold,
                color = headerColor
            )
            MoneyLine(type = t.type, amount = t.amount, isFuture = isFuture)
            Text(
                text = t.date.format(DateFmtDMY),
                style = MaterialTheme.typography.bodySmall,
                color = dateColor
            )
            if (!t.note.isNullOrBlank()) {
                Text(
                    text = t.note!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = noteColor
                )
            }
        }
    }
}

/**
 * Строка суммы для конкретной транзакции в списке месяца.
 * Доход всегда с плюсом, расход — с минусом.
 * Будущие транзакции показываются серым цветом.
 */
@Composable
private fun MoneyLine(type: TxType, amount: Double, isFuture: Boolean) {
    val core = moneyFormatPlain(amount)
    val sign = if (type == TxType.INCOME) "+" else "-"
    val text = sign + core

    val baseColor = if (type == TxType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
    val color = if (isFuture) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        baseColor
    }

    Text(text, color = color, fontWeight = FontWeight.Medium)
}

/**
 * Строки "Доход:" / "Расход:" в блоке итогов.
 * Доход с плюсом, расход с минусом.
 */
@Composable
private fun MoneyLineLabel(label: String, type: TxType, amount: Double) {
    val core = moneyFormatPlain(amount)
    val sign = if (type == TxType.INCOME) "+" else "-"
    val text = sign + core
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
