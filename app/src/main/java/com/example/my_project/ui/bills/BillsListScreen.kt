package com.example.my_project.ui.bills

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.components.EmptyState

/**
 * Экран списка счетов (пока заглушка).
 * Параметры согласованы с навигацией.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsListScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (billId: String) -> Unit
) {
    // Временная заглушка-данные; потом заменишь на vm.getBills(propertyId)
    data class UiBill(val id: String, val title: String, val amount: String, val subtitle: String)
    val items = emptyList<UiBill>() // список пуст — покажем EmptyState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Счета") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                // Ваш EmptyState ожидает subtitle, а не supportingText
                EmptyState(
                    title = "Пока пусто",
                    message = "Нажми «+», чтобы добавить первый счёт"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.Top
            ) {
                items(items) { item ->
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.subtitle) },
                        trailingContent = { Text(item.amount) },
                        modifier = Modifier.clickable { onEdit(item.id) }
                    )
                }
            }
        }
    }
}