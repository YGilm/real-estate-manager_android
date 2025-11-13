@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.my_project.ui.RealEstateViewModel
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun EditPropertyScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit
) {
    val properties by vm.properties.collectAsState()
    val current = properties.firstOrNull { it.id == propertyId }

    // Локальные стейты для формы, инициализируем из current
    var name by remember(current?.id) { mutableStateOf(current?.name.orEmpty()) }
    var address by remember(current?.id) { mutableStateOf(current?.address.orEmpty()) }
    var rentText by remember(current?.id) {
        mutableStateOf(current?.monthlyRent?.toString().orEmpty())
    }

    // coverUri из БД — отдельное состояние, чтобы синхронно видеть и редактировать
    var coverUri by remember(current?.id) { mutableStateOf(current?.coverUri) }

    val context = LocalContext.current

    // Пикер изображения для аватара
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                // Разрешение на долгосрочный доступ к URI
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Если не дали persistable — всё равно используем строку, пока процесс жив
            }

            val uriStr = uri.toString()
            coverUri = uriStr               // сразу обновляем UI
            vm.setCover(propertyId, uriStr) // сохраняем в БД через VM
        }
    }

    // Если репозиторий обновит объект (например, после setCover) —
    // подтянем новое значение coverUri в локальный стейт.
    LaunchedEffect(current?.coverUri) {
        if (current?.coverUri != coverUri) {
            coverUri = current?.coverUri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование объекта") },
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== Аватар объекта с кнопкой редактирования =====
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUri.isNullOrBlank()) {
                        // Дефолтная заглушка
                        Icon(
                            imageVector = Icons.Filled.Edit, // можешь заменить на Apartment
                            contentDescription = "Аватар объекта",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Аватар объекта",
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop // заполняем весь круг без полей
                        )
                    }
                }

                // Кнопка-карандаш в правом нижнем углу аватара
                IconButton(
                    onClick = {
                        imagePicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp) // чуть смещаем от края экрана
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Изменить аватар",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // ===== Поля формы =====
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название объекта") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Адрес") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rentText,
                onValueChange = { rentText = it },
                label = { Text("Месячная аренда, ₽") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена")
                }

                Button(
                    onClick = {
                        val rent = rentText.toDoubleOrNull()
                        vm.updateProperty(
                            id = propertyId,
                            name = name,
                            address = address.ifBlank { null },
                            monthlyRent = rent
                        )
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}