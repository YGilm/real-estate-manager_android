package com.example.my_project.ui.bills

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.my_project.ui.components.EmptyState

/**
 * Экран списка счетов для конкретного объекта.
 *
 * Пока без реальных данных — только заглушка и кнопка "Добавить".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsListScreen(
    propertyId: String,
    onBack: () -> Unit,
    onOpenBill: (String?) -> Unit, // null => создать новый счёт
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Счета по объекту") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenBill(null) }
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                Text(text = "Добавить счёт")
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Пока просто заглушка — потом сюда придёт список счетов
            EmptyState(
                title = "Счета пока не добавлены",
                message = "Нажми «Добавить счёт», чтобы создать первый счёт для этого объекта."
            )
        }
    }
}