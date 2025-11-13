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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.my_project.ui.RealEstateViewModel

@Composable
fun PropertyDetailsScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onEditProperty: () -> Unit,
    onOpenStatsForProperty: () -> Unit,
    onOpenBills: () -> Unit,
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
                    // Только редактирование — без дублирующей иконки "Счета"
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
            onEditProperty = onEditProperty
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
    onEditProperty: () -> Unit
) {
    // Берём объект из VM по id
    val properties by vm.properties.collectAsState()
    val property = properties.firstOrNull { it.id == propertyId }

    val titleText = property?.name?.takeIf { it.isNotBlank() } ?: "Объект недвижимости"
    val addressText = property?.address?.takeIf { it.isNotBlank() }
    val coverUri = property?.coverUri

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
                imageUrl = coverUri
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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

        // ===== Кнопки: 2 ряда по 2 =====
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
                onClick = {
                    // TODO: навигация к транзакциям по объекту
                },
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
                // При желании можно заменить на иконку статистики
                Icon(Icons.Filled.ReceiptLong, contentDescription = null)
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
    imageUrl: String? = null
) {
    val context = LocalContext.current

    if (!imageUrl.isNullOrBlank()) {
        // Показываем сохранённую картинку объекта
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            // Чтобы не было полос по бокам — заполняем круг полностью
            contentScale = ContentScale.FillBounds
        )
    } else {
        // Дефолтная иконка, если аватар ещё не загружен
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
}