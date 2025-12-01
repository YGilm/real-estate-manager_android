@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.my_project.ui.RealEstateViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PropertyInfoScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val property = vm.properties.collectAsState().value.firstOrNull { it.id == propertyId }

    val details by vm.propertyDetails(propertyId).collectAsState(initial = null)
    val photos by vm.propertyPhotos(propertyId).collectAsState(initial = emptyList())
    val docs by vm.attachments(propertyId).collectAsState(initial = emptyList())

    var isEditing by rememberSaveable(propertyId) { mutableStateOf(false) }

    // Draft поля (используются ТОЛЬКО в режиме редактирования)
    var draftDescription by rememberSaveable(propertyId) { mutableStateOf("") }
    var draftArea by rememberSaveable(propertyId) { mutableStateOf("") }

    // При входе в режим редактирования — подгружаем текущие значения в draft
    LaunchedEffect(isEditing) {
        if (isEditing) {
            draftDescription = details?.description.orEmpty()
            draftArea = details?.areaSqm.orEmpty()
        }
    }

    // ----------- Просмотр фото (диалог) -----------
    var photoViewerOpen by remember { mutableStateOf(false) }
    var photoViewerIndex by remember { mutableStateOf(0) }

    if (photoViewerOpen && photos.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { photoViewerOpen = false },
            confirmButton = {},
            dismissButton = {},
            title = {
                Text("Фото ${photoViewerIndex + 1} из ${photos.size}")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val current = photos.getOrNull(photoViewerIndex)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(current?.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        contentScale = ContentScale.Crop
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (photoViewerIndex > 0) photoViewerIndex -= 1
                            },
                            enabled = photoViewerIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Назад")
                        }

                        TextButton(
                            onClick = {
                                if (photoViewerIndex < photos.lastIndex) photoViewerIndex += 1
                            },
                            enabled = photoViewerIndex < photos.lastIndex
                        ) {
                            Text("Вперёд")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        )
    }

    // ----------- Фото (добавление) -----------
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {}
            }
            vm.addPropertyPhotos(propertyId, uris.map { it.toString() })
        }
    )

    // ----------- Документы (добавление) -----------
    var pendingDocUri by remember { mutableStateOf<Uri?>(null) }
    var pendingDocName by rememberSaveable { mutableStateOf("") }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (!isEditing || uri == null) return@rememberLauncherForActivityResult
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            pendingDocUri = uri
            pendingDocName = ""
        }
    )

    if (pendingDocUri != null) {
        AlertDialog(
            onDismissRequest = { pendingDocUri = null },
            title = { Text("Название документа") },
            text = {
                OutlinedTextField(
                    value = pendingDocName,
                    onValueChange = { pendingDocName = it },
                    singleLine = true,
                    placeholder = { Text("Например: Договор аренды") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingDocUri ?: return@TextButton
                        val name = pendingDocName.trim().ifBlank { "Документ" }
                        val mime = context.contentResolver.getType(uri)
                        vm.addAttachment(propertyId, name, mime, uri.toString())
                        pendingDocUri = null
                    }
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDocUri = null }) { Text("Отмена") }
            }
        )
    }

    // ----------- Заглушка арендатора -----------
    var showTenantStub by remember { mutableStateOf(false) }
    if (showTenantStub) {
        AlertDialog(
            onDismissRequest = { showTenantStub = false },
            icon = { Icon(Icons.Filled.Pets, contentDescription = null, modifier = Modifier.size(72.dp)) },
            title = { Text("Котик грустит 😿") },
            text = { Text("Функционал арендатора пока не готов. Мы работаем над этим, чтобы котик не грустил!") },
            confirmButton = { TextButton(onClick = { showTenantStub = false }) { Text("Ок") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(property?.name ?: "Детали объекта") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
                        }
                    } else {
                        TextButton(
                            onClick = {
                                vm.savePropertyDetails(
                                    propertyId = propertyId,
                                    description = draftDescription.trim().takeIf { it.isNotBlank() },
                                    areaSqm = normalizeArea(draftArea)
                                )
                                isEditing = false
                                scope.launch { snackbarHostState.showSnackbar("Сохранено") }
                            }
                        ) { Text("Сохранить") }

                        TextButton(onClick = { isEditing = false }) { Text("Отмена") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // Паспорт объекта
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.HomeWork, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Паспорт объекта", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.height(6.dp))

                    KeyValueRow(
                        "Арендная ставка",
                        property?.monthlyRent?.let { "${formatMoney(it)} ₽" } ?: "—"
                    )

                    val rent = property?.monthlyRent
                    val areaVal = parseArea(details?.areaSqm)
                    val perSqm = if (rent != null && areaVal != null && areaVal > 0.0) rent / areaVal else null

                    Column {
                        KeyValueRow(
                            "Ставка за м²",
                            when {
                                perSqm != null -> "${formatMoney(perSqm)} ₽/м²"
                                areaVal == null -> "Задайте метраж для расчёта"
                                else -> "—"
                            }
                        )
                        Text(
                            text = "в месяц",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    val lease = buildString {
                        val from = property?.leaseFrom?.takeIf { it.isNotBlank() }
                        val to = property?.leaseTo?.takeIf { it.isNotBlank() }
                        when {
                            from != null && to != null -> append("$from — $to")
                            from != null -> append("с $from")
                            to != null -> append("до $to")
                            else -> append("—")
                        }
                    }
                    KeyValueRow("Срок аренды", lease)
                }
            }

            // Описание
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Описание", style = MaterialTheme.typography.titleMedium)

                    if (!isEditing) {
                        val text = details?.description?.takeIf { it.isNotBlank() } ?: "—"
                        Text(text, color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        OutlinedTextField(
                            value = draftDescription,
                            onValueChange = { draftDescription = it },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Например: 2 комнаты, после ремонта, вид во двор…") }
                        )
                    }
                }
            }

            // Метраж
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Метраж", style = MaterialTheme.typography.titleMedium)

                    if (!isEditing) {
                        Text(
                            text = formatArea(details?.areaSqm) ?: "—",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        OutlinedTextField(
                            value = draftArea,
                            onValueChange = { draftArea = it },
                            singleLine = true,
                            label = { Text("м²") },
                            placeholder = { Text("Например: 45.50") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        formatArea(normalizeArea(draftArea))?.let { pretty ->
                            Text("Итог: $pretty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Фото
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Фото", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

                        // ✅ Скрепка — добавление фото ДОСТУПНО ВСЕГДА (без включения режима редактирования)
                        IconButton(onClick = { photoPicker.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Добавить фото")
                        }
                    }

                    if (photos.isEmpty()) {
                        Text("Фото пока не добавлены", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(photos, key = { it.id }) { p ->
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clickable {
                                            photoViewerIndex = photos.indexOfFirst { it.id == p.id }.coerceAtLeast(0)
                                            photoViewerOpen = true
                                        }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(p.uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // ✅ Иконка "увеличить" — визуальная подсказка, что фото кликабельное
                                    Icon(
                                        imageVector = Icons.Filled.ZoomIn,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp),
                                        tint = MaterialTheme.colorScheme.surface
                                    )

                                    // ✅ Удаление — у КАЖДОЙ фотки, но только в режиме редактирования (как было по логике)
                                    if (isEditing) {
                                        IconButton(
                                            onClick = { vm.deletePropertyPhoto(p.id) },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Удалить фото")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Документы
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Документы", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

                        if (isEditing) {
                            IconButton(onClick = { docPicker.launch(arrayOf("*/*")) }) {
                                Icon(Icons.Filled.AttachFile, contentDescription = "Добавить документ")
                            }
                        }
                    }

                    if (docs.isEmpty()) {
                        Text("Документы пока не добавлены", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        docs.forEach { a ->
                            DocRow(
                                name = a.name,
                                mimeType = a.mimeType,
                                onOpen = { openAttachment(context, a.uri, a.mimeType) },
                                onDelete = if (isEditing) ({ vm.deleteAttachment(a.id) }) else null
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Арендатор (пока заглушка)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Арендатор", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showTenantStub = true }) {
                        Text("Открыть арендатора")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DocRow(
    name: String?,
    mimeType: String?,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name?.takeIf { it.isNotBlank() } ?: "Документ", maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!mimeType.isNullOrBlank()) {
                Text(
                    mimeType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Filled.AttachFile, contentDescription = "Открыть")
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
    }
}

private fun openAttachment(context: android.content.Context, uriStr: String, mimeType: String?) {
    runCatching {
        val uri = Uri.parse(uriStr)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (!mimeType.isNullOrBlank()) setDataAndType(uri, mimeType) else data = uri
        }
        context.startActivity(intent)
    }
}

/** "45,5" -> "45.50". Пусто/0 -> null */
private fun normalizeArea(raw: String): String? {
    val cleaned = raw.trim().replace(',', '.')
    if (cleaned.isBlank()) return null
    val v = cleaned.toDoubleOrNull() ?: return null
    if (v == 0.0) return null
    return String.format(Locale.US, "%.2f", v)
}

/** Парс метража из строки модели (может быть "45,50" / "45.50"). */
private fun parseArea(areaSqm: String?): Double? {
    val s = areaSqm?.trim()?.replace(',', '.') ?: return null
    if (s.isBlank()) return null
    val v = s.toDoubleOrNull() ?: return null
    return if (v == 0.0) null else v
}

private fun formatArea(areaSqm: String?): String? {
    val v = areaSqm?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: return null
    if (v == 0.0) return null
    val s = String.format(Locale("ru", "RU"), "%.2f", v)
    return "$s м²"
}

private fun formatMoney(v: Double): String =
    String.format(Locale("ru", "RU"), "%,.0f", v).replace('\u00A0', ' ')