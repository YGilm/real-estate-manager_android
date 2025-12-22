package com.example.my_project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.my_project.data.model.Property
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.util.copyUriToAppStorage
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesListScreen(
    vm: RealEstateViewModel,
    onAdd: () -> Unit,
    onOpen: (id: String) -> Unit,
    onBack: () -> Unit
) {
    val properties by vm.properties.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(properties) {
        properties.forEach { property ->
            val uri = property.coverUri ?: return@forEach
            if (uri.startsWith("content://")) {
                val newUri = runCatching {
                    copyUriToAppStorage(context, Uri.parse(uri), "covers")
                }.getOrNull()
                if (!newUri.isNullOrBlank() && newUri != uri) {
                    vm.setCover(property.id, newUri)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Объекты недвижимости") },
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
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить объект")
            }
        }
    ) { inner ->
        if (properties.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Пока нет объектов")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(properties, key = { it.id }) { property ->
                    PropertyListItem(
                        property = property,
                        onClick = { onOpen(property.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyListItem(
    property: Property,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PropertyAvatarSmall(imageUrl = property.coverUri)

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = property.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                property.address?.takeIf { it.isNotBlank() }?.let { addr ->
                    Text(
                        text = addr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                buildLeaseText(property.leaseFrom, property.leaseTo)?.let { leaseText ->
                    Text(
                        text = leaseText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyAvatarSmall(
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
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(48.dp)
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

private fun buildLeaseText(from: String?, to: String?): String? {
    val fromClean = from?.takeIf { it.isNotBlank() }
    val toClean = to?.takeIf { it.isNotBlank() }

    return when {
        fromClean == null && toClean == null -> "Договор аренды не указан"
        fromClean != null && toClean != null -> "Договор аренды: с $fromClean по $toClean"
        fromClean != null -> "Договор аренды: с $fromClean"
        else -> "Договор аренды: по $toClean"
    }
}
