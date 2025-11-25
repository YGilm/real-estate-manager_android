@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.my_project.ui.RealEstateViewModel

@Composable
fun EditPropertyScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var rentText by remember { mutableStateOf("") }
    var leaseFromText by remember { mutableStateOf("") }
    var leaseToText by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(propertyId) {
        val p = vm.getProperty(propertyId)
        if (p != null) {
            name = p.name
            address = p.address.orEmpty()
            rentText = p.monthlyRent?.toString().orEmpty()
            leaseFromText = p.leaseFrom.orEmpty()
            leaseToText = p.leaseTo.orEmpty()
            coverUri = p.coverUri
        }
        isLoading = false
    }

    val context = LocalContext.current
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriString = uri.toString()
            coverUri = uriString
            vm.setCover(propertyId, uriString)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактировать объект") },
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(inner)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Аватар / обложка
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EditPropertyAvatar(coverUri = coverUri)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { pickImageLauncher.launch("image/*") }) {
                        Text("Изменить фото")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название объекта") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес объекта") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it },
                    label = { Text("Ежемесячная аренда, ₽") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = leaseFromText,
                    onValueChange = { leaseFromText = it },
                    label = { Text("Договор аренды: с (дата)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = leaseToText,
                    onValueChange = { leaseToText = it },
                    label = { Text("Договор аренды: по (дата)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
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
                                monthlyRent = rent,
                                leaseFrom = leaseFromText.ifBlank { null },
                                leaseTo = leaseToText.ifBlank { null }
                            )
                            onBack()
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPropertyAvatar(
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}