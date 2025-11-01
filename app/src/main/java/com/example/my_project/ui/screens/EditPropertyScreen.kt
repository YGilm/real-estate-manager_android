@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.my_project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.my_project.ui.RealEstateViewModel

@Composable
fun EditPropertyScreen(
    vm: RealEstateViewModel,
    propertyId: String,
    onBack: () -> Unit
) {
    var initialLoaded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf<String?>(null) }
    var rentText by remember { mutableStateOf("") }

    LaunchedEffect(propertyId) {
        val p = vm.getProperty(propertyId)
        if (p != null) {
            name = p.name
            address = p.address
            rentText = p.monthlyRent?.toString() ?: ""
        }
        initialLoaded = true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Редактировать объект") }) }
    ) { padding ->
        if (!initialLoaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address ?: "",
                    onValueChange = { address = it.ifBlank { null } },
                    label = { Text("Адрес") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it },
                    label = { Text("Аренда в месяц") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val rent = rentText.replace(",", ".").toDoubleOrNull()
                        vm.updateProperty(
                            id = propertyId,
                            name = name.trim(),
                            address = address?.trim(),
                            monthlyRent = rent
                        )
                        onBack()
                    }) { Text("Сохранить") }

                    OutlinedButton(onClick = onBack) { Text("Отмена") }
                }

                HorizontalDivider()

                Button(
                    onClick = {
                        vm.deleteProperty(propertyId)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить объект", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}