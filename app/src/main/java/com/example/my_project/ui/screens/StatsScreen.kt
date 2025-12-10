@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.util.Totals
import com.example.my_project.ui.util.computeTotals
import com.example.my_project.ui.util.moneyFormat
import com.example.my_project.ui.util.moneyFormatPlain
import com.example.my_project.ui.util.monthName
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    vm: RealEstateViewModel,
    onBack: () -> Unit,
    preselectedPropertyId: String? = null,
    onOpenMonth: (year: Int, month: Int, propertyId: String?) -> Unit = { _, _, _ -> }
) {
    val txs by vm.transactions.collectAsState()
    val properties by vm.properties.collectAsState()

    var selectedPropertyId by rememberSaveable { mutableStateOf<String?>(preselectedPropertyId) }
    // 0 = Месяц, 1 = Год, 2 = Всё время
    var tab by rememberSaveable { mutableStateOf(1) }

    LaunchedEffect(preselectedPropertyId) {
        if (!preselectedPropertyId.isNullOrBlank()) {
            selectedPropertyId = preselectedPropertyId
        }
    }

    val filteredTxs = remember(txs, selectedPropertyId) {
        if (selectedPropertyId.isNullOrBlank()) txs
        else txs.filter { it.propertyId == selectedPropertyId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
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
            PropertyFilterRow(
                properties = properties,
                selectedPropertyId = selectedPropertyId,
                onSelected = { selectedPropertyId = it }
            )

            Spacer(Modifier.height(12.dp))

            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Месяц") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Год") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Всё время") })
            }

            Spacer(Modifier.height(16.dp))

            when (tab) {
                0 -> MonthTab(txs = filteredTxs)
                1 -> YearTab(
                    txs = filteredTxs,
                    onOpenMonth = { y, m -> onOpenMonth(y, m, selectedPropertyId) }
                )
                2 -> AllTimeTab(txs = filteredTxs)
            }
        }
    }
}

@Composable
private fun PropertyFilterRow(
    properties: List<Property>,
    selectedPropertyId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = remember(selectedPropertyId, properties) {
        if (selectedPropertyId == null) "Все объекты"
        else properties.find { it.id == selectedPropertyId }?.name ?: "Все объекты"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Объект:", fontWeight = FontWeight.SemiBold)

        Box {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                maxLines = 1,
                label = { Text("Фильтр") },
                trailingIcon = {
                    val icon =
                        if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
                    Icon(
                        imageVector = icon,
                        contentDescription = "Раскрыть",
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { expanded = !expanded }
                    )
                },
                modifier = Modifier
                    .clickable { expanded = true }
                    .fillMaxWidth(0.6f)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Все объекты") },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    }
                )
                properties.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.name) },
                        onClick = {
                            onSelected(p.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/* ===== Вкладка «Месяц» ===== */

@Composable
private fun MonthTab(txs: List<Transaction>) {
    val now = remember { LocalDate.now() }

    // Доступные года из транзакций (или текущий год, если данных нет)
    val years = remember(txs) {
        txs.map { it.date.year }
            .distinct()
            .sortedDescending()
            .ifEmpty { listOf(now.year) }
    }

    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    var selectedYear by rememberSaveable(years) {
        mutableStateOf(if (years.contains(now.year)) now.year else years.first())
    }

    // Месяцы, в которых есть транзакции в выбранном году (если пусто — 1..12)
    val monthsForYear = remember(txs, selectedYear) {
        txs.filter { it.date.year == selectedYear }
            .map { it.date.monthValue }
            .distinct()
            .sorted()
            .ifEmpty { (1..12).toList() }
    }

    var selectedMonth by rememberSaveable(selectedYear) {
        mutableStateOf(
            if (selectedYear == now.year && monthsForYear.contains(now.monthValue)) now.monthValue
            else monthsForYear.first()
        )
    }

    // Если после смены года выбранный месяц недоступен — поправим
    if (!monthsForYear.contains(selectedMonth)) {
        selectedMonth = monthsForYear.first()
    }

    // Выбор года / месяца
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = selectedYear.toString(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Год") },
                trailingIcon = {
                    val icon =
                        if (yearExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.clickable { yearExpanded = !yearExpanded }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { yearExpanded = true }
            )

            DropdownMenu(
                expanded = yearExpanded,
                onDismissRequest = { yearExpanded = false }
            ) {
                years.forEach { y ->
                    DropdownMenuItem(
                        text = { Text(y.toString()) },
                        onClick = {
                            selectedYear = y
                            yearExpanded = false
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = monthName(selectedMonth),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Месяц") },
                trailingIcon = {
                    val icon =
                        if (monthExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.clickable { monthExpanded = !monthExpanded }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { monthExpanded = true }
            )

            DropdownMenu(
                expanded = monthExpanded,
                onDismissRequest = { monthExpanded = false }
            ) {
                monthsForYear.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(monthName(m)) },
                        onClick = {
                            selectedMonth = m
                            monthExpanded = false
                        }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // ВСЕ транзакции выбранного месяца (список внизу)
    val monthTxs = remember(txs, selectedYear, selectedMonth) {
        txs.filter { it.date.year == selectedYear && it.date.monthValue == selectedMonth }
            .sortedByDescending { it.date }
    }

    // Итоги — считаем через computeTotals(), который уже игнорирует будущие транзакции
    val totals = remember(monthTxs) { monthTxs.computeTotals() }

    Text(
        "Месяц: ${monthName(selectedMonth)} $selectedYear",
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))
    TotalsBlock(totals)

    Spacer(Modifier.height(16.dp))
    Text("Транзакции", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    if (monthTxs.isEmpty()) {
        Text(
            "Нет транзакций за выбранный месяц",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(monthTxs) { t ->
            val today = LocalDate.now()
            val isFuture = t.date.isAfter(today)

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = if (t.type == TxType.INCOME) "Доход" else "Расход",
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFuture)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    MonthTxMoneyLine(
                        type = t.type,
                        amount = t.amount,
                        isFuture = isFuture
                    )
                    Text(
                        text = t.date.format(dateFmt),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFuture)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!t.note.isNullOrBlank()) {
                        Text(
                            text = t.note!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFuture)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/* ===== Вкладка «Год» (кликабельные месяцы) ===== */

@Composable
private fun YearTab(
    txs: List<Transaction>,
    onOpenMonth: (year: Int, month: Int) -> Unit
) {
    val years = remember(txs) {
        txs.map { it.date.year }.distinct().sortedDescending()
            .ifEmpty { listOf(LocalDate.now().year) }
    }
    var selectedYear by rememberSaveable(years) { mutableStateOf(years.first()) }

    data class MonthRow(val month: Int, val totals: Totals)

    val byMonth = remember(txs, selectedYear) {
        (1..12).map { m ->
            val monthTx = txs.filter {
                it.date.year == selectedYear && it.date.monthValue == m
            }
            MonthRow(month = m, totals = monthTx.computeTotals())
        }
    }

    val yearTotals = remember(byMonth) {
        Totals(
            income = byMonth.sumOf { it.totals.income },
            expense = byMonth.sumOf { it.totals.expense }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Год:", fontWeight = FontWeight.SemiBold)

        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedTextField(
                value = selectedYear.toString(),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                maxLines = 1,
                label = { Text("Год") },
                trailingIcon = {
                    val icon =
                        if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown
                    Icon(
                        imageVector = icon,
                        contentDescription = "Раскрыть",
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { expanded = !expanded }
                    )
                },
                modifier = Modifier
                    .clickable { expanded = true }
                    .fillMaxWidth(0.4f)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                years.forEach { y ->
                    DropdownMenuItem(
                        text = { Text(y.toString()) },
                        onClick = { selectedYear = y; expanded = false }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    TotalsBlock(yearTotals)

    Spacer(Modifier.height(16.dp))
    Text("По месяцам", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(byMonth) { row ->
            ElevatedCard(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMonth(selectedYear, row.month) }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(monthName(row.month), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        MoneyLineLabel("Доход:", TxType.INCOME, row.totals.income)
                        MoneyLineLabel("Расход:", TxType.EXPENSE, row.totals.expense)
                        MoneyLineTotal("Итого:", row.totals.total)
                    }
                }
            }
        }
    }
}

/* ===== Вкладка «Всё время» ===== */

@Composable
private fun AllTimeTab(txs: List<Transaction>) {
    val totals = remember(txs) { txs.computeTotals() }
    Text("За всё время", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    TotalsBlock(totals)
}

/* ===== Общие UI-компоненты для денег ===== */

@Composable
private fun TotalsBlock(t: Totals) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MoneyLineLabel("Доход:", TxType.INCOME, t.income)
        MoneyLineLabel("Расход:", TxType.EXPENSE, t.expense)
        MoneyLineTotal("Итого:", t.total)
    }
}

/**
 * Строка суммы в списке месячных транзакций (MonthTab).
 * Доход с плюсом, расход с минусом, будущие — серым.
 */
@Composable
private fun MonthTxMoneyLine(
    type: TxType,
    amount: Double,
    isFuture: Boolean,
) {
    val core = moneyFormatPlain(amount)
    val sign = if (type == TxType.INCOME) "+" else "-"
    val text = sign + core

    val baseColor = if (type == TxType.INCOME) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFFC62828)
    }

    val color = if (isFuture) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    } else {
        baseColor
    }

    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Используется в TotalsBlock / YearTab, там уже нет будущих транзакций.
 */
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