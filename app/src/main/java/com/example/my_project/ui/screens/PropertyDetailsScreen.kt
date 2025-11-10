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
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.my_project.ui.RealEstateViewModel

@Composable
fun PropertyDetailsScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onEditProperty: () -> Unit,
    onOpenStatsForProperty: () -> Unit,
    onOpenBills: () -> Unit,
    // ↓ Новые необязательные параметры — чтобы убрать любые обращения к state
    propertyName: String? = null,
    propertyAddress: String? = null
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
                    IconButton(onClick = onOpenBills) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = "Счета"
                        )
                    }
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
            propertyName = propertyName,
            propertyAddress = propertyAddress
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
    propertyName: String?,
    propertyAddress: String?
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ===== Шапка с аватаром и названием =====
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PropertyAvatar(
                name = propertyName,
                imageUrl = null // когда появится URL — заменим на AsyncImage(CoIL)
            )
            Spacer(Modifier.height(12.dp))

            val titleText = propertyName?.takeIf { it.isNotBlank() } ?: "Объект недвижимости"
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val addressText = propertyAddress?.takeIf { it.isNotBlank() }
            if (addressText != null) {
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ===== Кнопки: 2 ряда по 2, симметрия и иконки =====
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
                onClick = { /* TODO: навигация к транзакциям по объекту */ },
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
                Icon(Icons.Filled.ReceiptLong, contentDescription = null) // при желании замени на иконку статистики
                Spacer(Modifier.size(8.dp))
                Text("Статистика")
            }
        }

        // Здесь можно добавить остальной контент деталей объекта
    }
}

@Composable
private fun PropertyAvatar(
    modifier: Modifier = Modifier,
    name: String? = null,
    imageUrl: String? = null
) {
    // Пока дефолтная иконка; заменим на Coil AsyncImage, если появится imageUrl
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}