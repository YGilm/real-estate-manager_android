package com.example.my_project.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    vm: RealEstateViewModel,
    onBack: () -> Unit
) {
    val tx = vm.transactions.collectAsState().value
    val totalIncome = tx.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val totalExpense = tx.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val net = totalIncome - totalExpense

    val ym = YearMonth.now()
    val thisMonth = tx.filter { it.date.year == ym.year && it.date.month == ym.month }
    val mIncome = thisMonth.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val mExpense = thisMonth.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val mNet = mIncome - mExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Итого за всё время:")
            Text("Доходы: ${"%,.2f".format(totalIncome)} ₽")
            Text("Расходы: ${"%,.2f".format(totalExpense)} ₽")
            Text("Чистая: ${"%,.2f".format(net)} ₽")
            Divider()
            Text("За ${ym.month} ${ym.year}:")
            Text("Доходы: ${"%,.2f".format(mIncome)} ₽")
            Text("Расходы: ${"%,.2f".format(mExpense)} ₽")
            Text("Чистая: ${"%,.2f".format(mNet)} ₽")
        }
    }
}