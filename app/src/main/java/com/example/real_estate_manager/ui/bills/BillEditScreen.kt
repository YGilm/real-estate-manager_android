package com.example.real_estate_manager.ui.bills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Экран создания/редактирования счёта.
 *
 * Пока без связи с БД/репозиторием — чистая UI-заглушка,
 * чтобы навигация и параметры компилировались.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEditScreen(
    propertyId: String,
    billId: String?,          // null => новый счёт
    onBack: () -> Unit
) {
    val isNew = billId == null

    // Простые локальные стейты (заглушка)
    val titleState = remember { mutableStateOf("") }
    val amountState = remember { mutableStateOf("") }
    val noteState = remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Новый счёт" else "Редактирование счёта") },
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
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Объект: $propertyId")

            OutlinedTextField(
                value = titleState.value,
                onValueChange = { titleState.value = it },
                label = { Text("Название счёта") },
                modifier = Modifier.fillMaxSize(fraction = 1f)
            )

            OutlinedTextField(
                value = amountState.value,
                onValueChange = { amountState.value = it },
                label = { Text("Сумма") }
            )

            OutlinedTextField(
                value = noteState.value,
                onValueChange = { noteState.value = it },
                label = { Text("Комментарий") }
            )

            Button(
                onClick = {
                    // TODO: здесь позже вызовем сохранение в ViewModel/репозиторий
                    onBack()
                }
            ) {
                Text(if (isNew) "Создать" else "Сохранить")
            }
        }
    }
}