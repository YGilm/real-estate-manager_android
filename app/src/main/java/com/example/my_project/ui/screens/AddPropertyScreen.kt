@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.example.my_project.ui.util.copyUriToAppStorage

@Composable
fun AddPropertyScreen(
    onSave: (
        name: String,
        address: String?,
        monthlyRent: Double?,
        leaseFrom: String?,
        leaseTo: String?,
        coverUri: String?
    ) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var rentText by remember { mutableStateOf("") }
    var leaseFrom by remember { mutableStateOf("") }
    var leaseTo by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<String?>(null) }

    // Выбор картинки для аватара
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                // Если не дали persistable — всё равно используем строку, пока процесс жив
            }
            coverUri = copyUriToAppStorage(context, uri, "covers") ?: uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый объект") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Блок аватара
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.material3.MaterialTheme
                                .colorScheme.surfaceVariant
                        )
                        .clickable { pickImageLauncher.launch(arrayOf("image/*")) },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(coverUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Apartment,
                            contentDescription = null
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Аватар объекта")
                    TextButton(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text("Выбрать изображение")
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Адрес (необязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = rentText,
                onValueChange = { rentText = it },
                label = { Text("Аренда в месяц (необязательно)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = leaseFrom,
                onValueChange = { leaseFrom = it },
                label = { Text("Договор аренды с (дата)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = leaseTo,
                onValueChange = { leaseTo = it },
                label = { Text("Договор аренды по (дата)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена")
                }
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val rent = rentText
                            .replace(" ", "")
                            .replace(",", ".")
                            .toDoubleOrNull()

                        onSave(
                            name.trim(),
                            address.ifBlank { null },
                            rent,
                            leaseFrom.ifBlank { null },
                            leaseTo.ifBlank { null },
                            coverUri
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}
