@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.util.DateFmtDMY
import com.example.my_project.ui.util.ReportPdfGenerator
import com.example.my_project.ui.util.Totals
import com.example.my_project.ui.util.buildPeriodReportText
import com.example.my_project.ui.util.computeTotals
import com.example.my_project.ui.util.computeTotalsInRange
import com.example.my_project.ui.util.inDateRange
import com.example.my_project.ui.util.moneyFormat
import com.example.my_project.ui.util.moneyFormatPlain
import com.example.my_project.ui.util.monthName
import com.example.my_project.ui.util.monthsInclusive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    // 0 = Месяц, 1 = Год, 2 = Всё время, 3 = Отчет за период
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

    val selectedPropertyName = remember(selectedPropertyId, properties) {
        if (selectedPropertyId.isNullOrBlank()) "Все объекты"
        else properties.find { it.id == selectedPropertyId }?.name ?: "Объект"
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

            ScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 0.dp
            ) {
                TabLabel(tab == 0, { tab = 0 }, "Месяц")
                TabLabel(tab == 1, { tab = 1 }, "Год")
                TabLabel(tab == 2, { tab = 2 }, "Всё время")
                TabLabel(tab == 3, { tab = 3 }, "Отчет за период")
            }

            Spacer(Modifier.height(16.dp))

            when (tab) {
                0 -> MonthTab(txs = filteredTxs)
                1 -> YearTab(
                    txs = filteredTxs,
                    onOpenMonth = { y, m -> onOpenMonth(y, m, selectedPropertyId) }
                )
                2 -> AllTimeTab(txs = filteredTxs)
                3 -> PeriodTab(
                    txs = filteredTxs,
                    propertyName = selectedPropertyName
                )
            }
        }
    }
}

@Composable
private fun TabLabel(
    selected: Boolean,
    onClick: () -> Unit,
    text: String
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Tab(
        selected = selected,
        onClick = onClick,
        selectedContentColor = selectedColor,
        unselectedContentColor = unselectedColor,
        modifier = Modifier.height(44.dp),
        text = {
            Text(
                text = text,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = if (selected) selectedColor else unselectedColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
            )
        }
    )
}

/* ===================== Вкладка «Отчет за период» (итоги + share + PDF) ===================== */

@Composable
private fun PeriodTab(
    txs: List<Transaction>,
    propertyName: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }
    val defaultFrom = remember(today) { today.withDayOfMonth(1) }

    var fromEpochDay by rememberSaveable { mutableStateOf(defaultFrom.toEpochDay()) }
    var toEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var showFromPicker by rememberSaveable { mutableStateOf(false) }
    var showToPicker by rememberSaveable { mutableStateOf(false) }

    val from = remember(fromEpochDay) { LocalDate.ofEpochDay(fromEpochDay) }
    val to = remember(toEpochDay) { LocalDate.ofEpochDay(toEpochDay) }

    val includeFuture = false

    val totals = remember(txs, fromEpochDay, toEpochDay) {
        txs.computeTotalsInRange(from, to, includeFuture = includeFuture)
    }

    val periodTxs = remember(txs, fromEpochDay, toEpochDay) {
        txs.inDateRange(from, to, includeFuture = includeFuture)
            .sortedByDescending { it.date }
    }

    val months = remember(fromEpochDay, toEpochDay) { monthsInclusive(from, to) }
    val avgNet = remember(totals, months) { totals.total / months }

    val reportText = remember(propertyName, fromEpochDay, toEpochDay, totals, avgNet) {
        buildPeriodReportText(
            propertyName = propertyName,
            from = from,
            to = to,
            totals = totals,
            avgNetPerMonth = avgNet,
            includeFuture = includeFuture
        )
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = from
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) {
                            val selected = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            fromEpochDay = selected.toEpochDay()
                            if (selected.isAfter(to)) toEpochDay = selected.toEpochDay()
                        }
                        showFromPicker = false
                    }
                ) { Text("Ок") }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) { Text("Отмена") }
            }
        ) { DatePicker(state = state) }
    }

    if (showToPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = to
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) {
                            val selected = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            toEpochDay = selected.toEpochDay()
                            if (selected.isBefore(from)) fromEpochDay = selected.toEpochDay()
                        }
                        showToPicker = false
                    }
                ) { Text("Ок") }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) { Text("Отмена") }
            }
        ) { DatePicker(state = state) }
    }

    Text("Отчет за период", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Период: ${from.format(DateFmtDMY)} — ${to.format(DateFmtDMY)}",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Транзакций: ${periodTxs.size} • Месяцев: $months",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Будущие транзакции не учитываются",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { showFromPicker = true }, modifier = Modifier.weight(1f)) {
                    Text("С: ${from.format(DateFmtDMY)}")
                }
                Button(onClick = { showToPicker = true }, modifier = Modifier.weight(1f)) {
                    Text("По: ${to.format(DateFmtDMY)}")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val subject =
                            "Отчёт: $propertyName (${from.format(DateFmtDMY)} — ${to.format(DateFmtDMY)})"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, "Поделиться отчётом"))
                        }.onFailure {
                            Toast.makeText(context, "Не удалось поделиться: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(Modifier.padding(start = 6.dp))
                    Text("Поделиться", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                val pdfFile = withContext(Dispatchers.IO) {
                                    ReportPdfGenerator.createPeriodPdfReport(
                                        context = context,
                                        propertyName = propertyName,
                                        from = from,
                                        to = to,
                                        transactionsInPeriod = periodTxs,
                                        totals = totals,
                                        avgNetPerMonth = avgNet,
                                        includeFuture = includeFuture
                                    )
                                }

                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    pdfFile
                                )

                                val subject =
                                    "PDF-отчёт: $propertyName (${from.format(DateFmtDMY)} — ${to.format(DateFmtDMY)})"

                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_SUBJECT, subject)
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(Intent.createChooser(intent, "Поделиться PDF-отчётом"))
                            }.onFailure {
                                Toast.makeText(context, "Ошибка экспорта PDF: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.padding(start = 6.dp))
                    Text("Экспорт PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    TotalsBlock(totals)
}

/* ===================== Фильтр по объектам ===================== */

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
                    .fillMaxWidth(0.65f)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Все объекты") },
                    onClick = { onSelected(null); expanded = false }
                )
                properties.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.name) },
                        onClick = { onSelected(p.id); expanded = false }
                    )
                }
            }
        }
    }
}

/* ===================== Вкладка «Месяц» ===================== */

@Composable
private fun MonthTab(txs: List<Transaction>) {
    val now = remember { LocalDate.now() }

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

    if (!monthsForYear.contains(selectedMonth)) {
        selectedMonth = monthsForYear.first()
    }

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
                        onClick = { selectedYear = y; yearExpanded = false }
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
                        onClick = { selectedMonth = m; monthExpanded = false }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    val monthTxs = remember(txs, selectedYear, selectedMonth) {
        txs.filter { it.date.year == selectedYear && it.date.monthValue == selectedMonth }
            .sortedByDescending { it.date }
    }

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
        Text("Нет транзакций за выбранный месяц", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(monthTxs) { t ->
            val isFuture = t.date.isAfter(LocalDate.now())
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
                        muted = isFuture
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
                        Spacer(Modifier.height(4.dp))
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

/* ===================== Вкладка «Год» ===================== */

@Composable
private fun YearTab(
    txs: List<Transaction>,
    onOpenMonth: (year: Int, month: Int) -> Unit
) {
    val years = remember(txs) {
        txs.map { it.date.year }
            .distinct()
            .sortedDescending()
            .ifEmpty { listOf(LocalDate.now().year) }
    }
    var selectedYear by rememberSaveable(years) { mutableStateOf(years.first()) }

    data class MonthRow(val month: Int, val totals: Totals)

    val byMonth = remember(txs, selectedYear) {
        (1..12).map { m ->
            val monthTx = txs.filter { it.date.year == selectedYear && it.date.monthValue == m }
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
                    .fillMaxWidth(0.42f)
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

/* ===================== Вкладка «Всё время» ===================== */

@Composable
private fun AllTimeTab(txs: List<Transaction>) {
    val totals = remember(txs) { txs.computeTotals() }
    Text("За всё время", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    TotalsBlock(totals)
}

/* ===================== Общие UI-компоненты денег ===================== */

@Composable
private fun TotalsBlock(t: Totals) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MoneyLineLabel("Доход:", TxType.INCOME, t.income)
        MoneyLineLabel("Расход:", TxType.EXPENSE, t.expense)
        MoneyLineTotal("Итого:", t.total)
    }
}

@Composable
private fun MonthTxMoneyLine(
    type: TxType,
    amount: Double,
    muted: Boolean = false
) {
    val core = moneyFormatPlain(amount)
    val sign = if (type == TxType.INCOME) "+" else "-"
    val text = sign + core

    val baseColor = if (type == TxType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
    val color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f) else baseColor

    Text(text = text, color = color, fontWeight = FontWeight.Medium)
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