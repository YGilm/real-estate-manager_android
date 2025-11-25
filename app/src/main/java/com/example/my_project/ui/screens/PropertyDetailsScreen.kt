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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.util.moneyFormatPlain
import java.time.LocalDate

@Composable
fun PropertyDetailsScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onEditProperty: () -> Unit,
    onOpenStatsForProperty: () -> Unit,
    onOpenBills: () -> Unit,
    onOpenTransactions: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Объект") },
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
    ) { inner ->
        PropertyDetailsContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            vm = vm,
            propertyId = propertyId,
            onOpenStatsForProperty = onOpenStatsForProperty,
            onOpenBills = onOpenBills,
            onEditProperty = onEditProperty,
            onOpenTransactions = onOpenTransactions
        )
    }
}

@Composable
private fun PropertyDetailsContent(
    modifier: Modifier,
    vm: RealEstateViewModel,
    propertyId: String,
    onOpenStatsForProperty: () -> Unit,
    onOpenBills: () -> Unit,
    onEditProperty: () -> Unit,
    onOpenTransactions: () -> Unit
) {
    val properties by vm.properties.collectAsState()
    val transactions by vm.transactions.collectAsState()

    val property = properties.firstOrNull { it.id == propertyId }

    val titleText = property?.name?.takeIf { it.isNotBlank() } ?: "Объект недвижимости"
    val addressText = property?.address?.takeIf { it.isNotBlank() }
    val coverUri = property?.coverUri
    val leaseText = buildLeaseText(property?.leaseFrom, property?.leaseTo)

    val currentYear = LocalDate.now().year
    val yearTransactions = transactions.filter {
        it.propertyId == propertyId && it.date.year == currentYear
    }
    val income = yearTransactions
        .filter { it.type == TxType.INCOME }
        .sumOf { it.amount }
    val expense = yearTransactions
        .filter { it.type == TxType.EXPENSE }
        .sumOf { it.amount }
    val total = income - expense

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Шапка
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PropertyAvatar(imageUrl = coverUri)

            Spacer(Modifier.height(12.dp))

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (addressText != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (leaseText != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = leaseText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedButton(
                onClick = onEditProperty,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Редактировать")
            }

            ElevatedButton(
                onClick = onOpenTransactions,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Транзакции")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedButton(
                onClick = onOpenBills,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Счета")
            }

            ElevatedButton(
                onClick = onOpenStatsForProperty,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Статистика")
            }
        }

        // Итоги
        if (yearTransactions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Итоги за $currentYear",
                        style = MaterialTheme.typography.titleMedium
                    )

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
                            text = moneyFormatPlain(expense),
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    androidx.compose.material3.Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Итого")
                        Text(
                            text = moneyFormatPlain(total),
                            color = when {
                                total > 0 -> Color(0xFF2E7D32)
                                total < 0 -> Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            Text(
                text = "За текущий год транзакций ещё нет",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PropertyAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier
                .size(96.dp)
                .clip(androidx.compose.foundation.shape.CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(96.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Apartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildLeaseText(from: String?, to: String?): String? {
    val fromClean = from?.takeIf { it.isNotBlank() }
    val toClean = to?.takeIf { it.isNotBlank() }

    return when {
        fromClean == null && toClean == null -> null
        fromClean != null && toClean != null -> "Договор аренды: с $fromClean по $toClean"
        fromClean != null -> "Договор аренды: с $fromClean"
        else -> "Договор аренды: по $toClean"
    }
}