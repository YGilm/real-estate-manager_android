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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.util.DateFmtDMY
import com.example.my_project.ui.util.Totals
import com.example.my_project.ui.util.computeTotals
import com.example.my_project.ui.util.moneyFormat
import com.example.my_project.ui.util.moneyFormatPlain
import com.example.my_project.ui.util.monthName

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
                .padding(16.dp)
        ) {
            TotalsRow(totals)
            Spacer(Modifier.height(16.dp))

            Text("Транзакции", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(txs) { t -> TxCard(t) }
            }
        }
    }
}

@Composable
private fun TotalsRow(t: Totals) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MoneyLineLabel("Доход:", TxType.INCOME, t.income)
        MoneyLineLabel("Расход:", TxType.EXPENSE, t.expense)
        MoneyLineTotal("Итого:", t.total)
    }
}

@Composable
private fun TxCard(t: Transaction) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                if (t.type == TxType.INCOME) "Доход" else "Расход",
                fontWeight = FontWeight.SemiBold
            )
            MoneyLine(type = t.type, amount = t.amount)
            Text(t.date.format(DateFmtDMY), style = MaterialTheme.typography.bodySmall)
            if (!t.note.isNullOrBlank()) {
                Text(t.note!!, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/* Локальные UI-хелперы для денег — такие же, как в StatsScreen */

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