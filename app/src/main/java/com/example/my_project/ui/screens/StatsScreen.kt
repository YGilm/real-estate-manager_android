@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.abs

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

    // 0 = Месяц, 1 = Год, 2 = Период
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

    val selectedProperty: Property? = remember(selectedPropertyId, properties) {
        if (selectedPropertyId.isNullOrBlank()) null
        else properties.find { it.id == selectedPropertyId }
    }

    val selectedPropertyName = remember(selectedPropertyId, properties) {
        if (selectedPropertyId.isNullOrBlank()) "Все объекты"
        else properties.find { it.id == selectedPropertyId }?.name ?: "Объект"
    }

    // ВАЖНО: аватар = coverUri
    val avatarUriForPdf: String? = remember(selectedProperty) { selectedProperty?.coverUri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Статистика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ✅ Только строка фильтра — на всю ширину, прижата к верху
                ObjectFilterRowFullWidth(
                    properties = properties,
                    selectedPropertyId = selectedPropertyId,
                    onSelected = { selectedPropertyId = it }
                )

                Spacer(Modifier.height(8.dp))

                StatsTabs(
                    selected = tab,
                    onSelect = { tab = it }
                )

                Spacer(Modifier.height(10.dp))

                // Контент вкладок занимает остаток и скроллится внутри
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> MonthTabContent(txs = filteredTxs)
                        1 -> YearTabContent(
                            txs = filteredTxs,
                            onOpenMonth = { y, m -> onOpenMonth(y, m, selectedPropertyId) }
                        )
                        2 -> PeriodTabContent(
                            txs = filteredTxs,
                            propertyName = selectedPropertyName,
                            propertyAvatarUri = avatarUriForPdf
                        )
                    }
                }
            }
        }
    }
}

/* ===================== Tabs (компактные) ===================== */

@Composable
private fun StatsTabs(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val labels = listOf("Месяц", "Год", "Период")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        TabRow(
            selectedTabIndex = selected,
            containerColor = Color.Transparent,
            indicator = { tabPositions ->
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selected])
                        .fillMaxHeight()
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            },
            divider = {}
        ) {
            labels.forEachIndexed { index, label ->
                val isSelected = selected == index
                Tab(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                    text = {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

/* ===================== Фильтр (одна строка, без плашки "Объект") ===================== */

@Composable
private fun ObjectFilterRowFullWidth(
    properties: List<Property>,
    selectedPropertyId: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedLabel = remember(selectedPropertyId, properties) {
        if (selectedPropertyId == null) "Все объекты"
        else properties.find { it.id == selectedPropertyId }?.name ?: "Все объекты"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            maxLines = 1,
            label = { Text("Фильтр по объекту") },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )

        ExposedDropdownMenu(
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

/* ===================== Период ===================== */

@Composable
private fun PeriodTabContent(
    txs: List<Transaction>,
    propertyName: String,
    propertyAvatarUri: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }

    val UNSET = Long.MIN_VALUE
    var fromEpochDay by rememberSaveable { mutableStateOf(UNSET) }
    var toEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }

    var showFromPicker by rememberSaveable { mutableStateOf(false) }
    var showToPicker by rememberSaveable { mutableStateOf(false) }

    val fromSelected = fromEpochDay != UNSET
    val from = remember(fromEpochDay) { if (fromSelected) LocalDate.ofEpochDay(fromEpochDay) else null }
    val to = remember(toEpochDay) { LocalDate.ofEpochDay(toEpochDay) }

    val includeFuture = false

    val periodTxs = remember(txs, fromEpochDay, toEpochDay) {
        if (!fromSelected) emptyList()
        else txs.inDateRange(from!!, to, includeFuture = includeFuture)
            .sortedByDescending { it.date }
    }

    val totals = remember(txs, fromEpochDay, toEpochDay) {
        if (!fromSelected) Totals(0.0, 0.0)
        else txs.computeTotalsInRange(from!!, to, includeFuture = includeFuture)
    }

    val months = remember(fromEpochDay, toEpochDay) {
        if (!fromSelected) 1 else monthsInclusive(from!!, to)
    }

    val avgNet = remember(totals, months) { totals.total / months }

    val reportText = remember(propertyName, fromEpochDay, toEpochDay, totals, avgNet) {
        if (!fromSelected) ""
        else buildPeriodReportText(
            propertyName = propertyName,
            from = from!!,
            to = to,
            totals = totals,
            avgNetPerMonth = avgNet,
            includeFuture = includeFuture
        )
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (from ?: today)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        fromEpochDay = selected.toEpochDay()
                        if (selected.isAfter(to)) toEpochDay = selected.toEpochDay()
                    }
                    showFromPicker = false
                }) { Text("Ок") }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Отмена") } }
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
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        toEpochDay = selected.toEpochDay()
                        if (fromSelected && selected.isBefore(from!!)) fromEpochDay = selected.toEpochDay()
                    }
                    showToPicker = false
                }) { Text("Ок") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    val actionsEnabled = fromSelected

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Отчет за период",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showFromPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (!fromSelected) "С: выбрать" else "С: ${from!!.format(DateFmtDMY)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { showToPicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "По: ${to.format(DateFmtDMY)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!fromSelected) {
                        Text(
                            text = "Выбери дату “С”, чтобы сформировать отчёт.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Транзакций: ${periodTxs.size} • Месяцев: $months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val subject =
                                    "Отчёт: $propertyName (${from!!.format(DateFmtDMY)} — ${to.format(DateFmtDMY)})"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, subject)
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(intent, "Поделиться отчётом"))
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "Не удалось поделиться: ${it.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            enabled = actionsEnabled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
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
                                                from = from!!,
                                                to = to,
                                                transactionsInPeriod = periodTxs,
                                                totals = totals,
                                                avgNetPerMonth = avgNet,
                                                includeFuture = false,
                                                avatarUri = propertyAvatarUri
                                            )
                                        }

                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            pdfFile
                                        )

                                        val subject =
                                            "PDF-отчёт: $propertyName (${from!!.format(DateFmtDMY)} — ${to.format(DateFmtDMY)})"

                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_SUBJECT, subject)
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Поделиться PDF-отчётом"))
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "Ошибка экспорта PDF: ${it.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = actionsEnabled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Экспорт PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        item {
            val showAvg = fromSelected && months >= 2
            TotalsBlock(
                t = totals,
                avgNetLabel = if (showAvg) "Средняя за месяц:" else null,
                avgNetValue = if (showAvg) avgNet else null
            )
        }
    }
}

/* ===================== Месяц ===================== */

@Composable
private fun MonthTabContent(txs: List<Transaction>) {
    val now = remember { LocalDate.now() }

    val years = remember(txs) {
        txs.map { it.date.year }
            .distinct()
            .sortedDescending()
            .ifEmpty { listOf(now.year) }
    }

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

    if (!monthsForYear.contains(selectedMonth)) selectedMonth = monthsForYear.first()

    val monthTxs = remember(txs, selectedYear, selectedMonth) {
        txs.filter { it.date.year == selectedYear && it.date.monthValue == selectedMonth }
            .sortedByDescending { it.date }
    }

    val totals = remember(monthTxs) { monthTxs.computeTotals() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SimpleDropdown(
                    label = "Год",
                    value = selectedYear.toString(),
                    items = years.map { it.toString() },
                    onSelect = { selectedYear = it.toInt() },
                    modifier = Modifier.weight(1f)
                )
                SimpleDropdown(
                    label = "Месяц",
                    value = monthName(selectedMonth),
                    items = monthsForYear.map { monthName(it) },
                    onSelect = { picked ->
                        val idx = monthsForYear.indexOfFirst { monthName(it) == picked }
                        if (idx >= 0) selectedMonth = monthsForYear[idx]
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            TotalsBlock(
                t = totals,
                avgNetLabel = null,
                avgNetValue = null
            )
        }
        item { Text("Транзакции", style = MaterialTheme.typography.titleMedium) }

        if (monthTxs.isEmpty()) {
            item { Text("Нет транзакций за выбранный месяц", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            itemsIndexed(monthTxs) { _, t ->
                val isFuture = t.date.isAfter(LocalDate.now())
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
                            color = if (isFuture)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                        MonthTxMoneyLine(type = t.type, amount = t.amount, muted = isFuture)
                        Text(
                            text = t.date.format(dateFmt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!t.note.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(text = t.note!!, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

/* ===================== Год ===================== */

@Composable
private fun YearTabContent(
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SimpleDropdown(
                label = "Год",
                value = selectedYear.toString(),
                items = years.map { it.toString() },
                onSelect = { selectedYear = it.toInt() },
                modifier = Modifier.fillMaxWidth(0.55f)
            )
        }

        item {
            TotalsBlock(
                t = yearTotals,
                avgNetLabel = null,
                avgNetValue = null
            )
        }
        item { Text("По месяцам", style = MaterialTheme.typography.titleMedium) }

        items(byMonth) { row ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMonth(selectedYear, row.month) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(monthName(row.month), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                        MoneyLineLabel("Доход:", TxType.INCOME, row.totals.income)
                        MoneyLineLabel("Расход:", TxType.EXPENSE, row.totals.expense)
                        MoneyLineTotal("Итого:", row.totals.total)
                    }
                }
            }
        }
    }
}

/* ===================== Простой dropdown (компактный) ===================== */

@Composable
private fun SimpleDropdown(
    label: String,
    value: String,
    items: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            maxLines = 1,
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = { onSelect(item); expanded = false }
                )
            }
        }
    }
}

/* ===================== Общие UI-компоненты денег ===================== */

@Composable
private fun TotalsBlock(
    t: Totals,
    avgNetLabel: String?,
    avgNetValue: Double?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MoneyLineLabel("Доход:", TxType.INCOME, t.income)
            MoneyLineLabel("Расход:", TxType.EXPENSE, t.expense)
            if (avgNetLabel != null && avgNetValue != null) {
                MoneyLineAverage(avgNetLabel, avgNetValue)
            }
            MoneyLineTotal("Итого:", t.total)
        }
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
    val text = if (type == TxType.EXPENSE && amount == 0.0) {
        moneyFormatPlain(amount)
    } else {
        moneyFormat(amount, type)
    }
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

@Composable
private fun MoneyLineAverage(label: String, amount: Double) {
    val text = if (amount < 0) {
        "-${moneyFormatPlain(abs(amount))}"
    } else {
        moneyFormatPlain(amount)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
