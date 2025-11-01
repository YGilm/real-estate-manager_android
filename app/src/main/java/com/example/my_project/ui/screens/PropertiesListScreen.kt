package com.example.my_project.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.my_project.ui.RealEstateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertiesListScreen(
    vm: RealEstateViewModel,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit
) {
    val items = vm.properties.collectAsState().value
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Объекты") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Пока пусто. Нажми «+» чтобы добавить объект.")
            }
        } else {
            LazyColumn(Modifier
                .fillMaxSize()
                .padding(padding)) {
                items(items) { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = {
                            val rent =
                                p.monthlyRent?.let { " • Аренда: ${"%,.2f".format(it)}" } ?: ""
                            Text((p.address ?: "Без адреса") + rent)
                        },
                        modifier = Modifier.clickable { onOpen(p.id) }
                    )
                    Divider()
                }
            }
        }
    }
}