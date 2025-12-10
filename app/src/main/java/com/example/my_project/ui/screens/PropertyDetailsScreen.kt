@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import java.time.LocalDate
import java.util.Locale

@Composable
fun PropertyDetailsScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onEditProperty: () -> Unit,
    onOpenDetails: () -> Unit,
    onOpenStatsForProperty: () -> Unit,
    onOpenBills: () -> Unit,
    onOpenTransactions: () -> Unit
) {
    val properties by vm.properties.collectAsState()
    val property = properties.firstOrNull { it.id == propertyId }

    val allTxs by vm.transactions.collectAsState()
    val today = remember { LocalDate.now() }
    val year = today.year

    // В годовой карточке объекта учитываем только транзакции,
    // дата которых уже наступила (date <= сегодня)
    val yearTransactions = allTxs
        .filter { it.propertyId == propertyId }
        .filter { it.date.year == year && !it.date.isAfter(today) }

    val income = yearTransactions.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = yearTransactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val total = income - expense

    var featureStubMessage by remember { mutableStateOf<String?>(null) }

    if (featureStubMessage != null) {
        AlertDialog(
            onDismissRequest = { featureStubMessage = null },
            icon = {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = "В разработке",
                    modifier = Modifier.size(72.dp)
                )
            },
            title = { Text("Упс...") },
            text = { Text( "Данный функционал в разработке") },
            confirmButton = {
                TextButton(onClick = { featureStubMessage = null }) {
                    Text("ок")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(property?.name ?: "Объект") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditProperty) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Редактировать"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        PropertyAvatar(
                            coverUri = property?.coverUri
                        )

                        Spacer(Modifier.size(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = property?.name ?: "Без названия",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!property?.address.isNullOrBlank()) {
                                Text(
                                    text = property?.address.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedButton(
                    onClick = onOpenDetails,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Детали")
                }

                ElevatedButton(
                    onClick = onOpenTransactions,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Транзакции")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedButton(
                    onClick = {
                        featureStubMessage =
                            "Потому что пока не может увидеть показания по этому объекту. " +
                                    "Мы уже работаем над этим функционалом, чтобы не расстраивать котика."
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Speed, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Показания")
                }

                ElevatedButton(
                    onClick = onOpenStatsForProperty,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Insights, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Статистика")
                }
            }

            // Статистика за год
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Статистика за $year год",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Доход")
                        Text(
                            text = moneyFormatPlain(income),
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Расход")
                        Text(
                            text = if (expense == 0.0) moneyFormatPlain(expense) else "-${moneyFormatPlain(expense)}",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Итого")
                        Text(
                            text = moneyFormatPlain(total),
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    val rentText = property?.monthlyRent?.let { "${moneyFormatPlain(it)} ₽" } ?: "—"
                    Text(
                        text = "Арендная ставка/мес: $rentText",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 1-в-1 как в EditPropertyScreen.kt:
 * - size(96.dp)
 * - CircleShape
 * - ContentScale.Crop
 */
@Composable
private fun PropertyAvatar(
    coverUri: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (!coverUri.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(coverUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier
                .size(96.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Apartment,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun moneyFormatPlain(value: Double): String {

    return String.format(Locale("ru", "RU"), "%,.0f", value)
        .replace('\u00A0', ' ')
}