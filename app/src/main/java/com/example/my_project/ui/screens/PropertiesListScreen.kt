package com.example.my_project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.util.moneyFormat
import com.example.my_project.ui.util.moneyFormatPlain
import com.example.my_project.ui.util.monthName
import java.time.LocalDate

@ExperimentalMaterial3Api
@Composable
fun PropertiesListScreen(
    vm: RealEstateViewModel,
    onAdd: () -> Unit,
    onOpen: (id: String) -> Unit,
    onBack: () -> Unit
) {
    val properties by vm.properties.collectAsState()
    val transactions by vm.transactions.collectAsState()

    val currentYear = remember { LocalDate.now().year }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Объекты недвижимости") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = onAdd) { Text("Добавить") }
                }
            )
        }
    ) { padding ->
        if (properties.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет объектов. Добавьте первый.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(properties) { p ->
                    val txsForYear = remember(transactions, p.id, currentYear) {
                        transactions.filter { it.propertyId == p.id && it.date.year == currentYear }
                    }
                    val income = txsForYear.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                    val expense =
                        txsForYear.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                    val total = income - expense

                    ElevatedCard(
                        onClick = { onOpen(p.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            // Заголовок: имя + подпись "Текущий год"
                            Text(
                                p.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Текущий год: $currentYear",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(10.dp))

                            // Суммы
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Доход:")
                                Text(
                                    moneyFormat(income, TxType.INCOME),
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Расход:")
                                Text(
                                    moneyFormat(expense, TxType.EXPENSE), // будет с минусом
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Итого:", fontWeight = FontWeight.SemiBold)
                                Text(
                                    moneyFormatPlain(total),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}