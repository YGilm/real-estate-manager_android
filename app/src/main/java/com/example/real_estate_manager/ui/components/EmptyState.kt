package com.example.real_estate_manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Универсальный пустой экран/состояние.
 *
 * @param icon иконка (опционально)
 * @param title заголовок
 * @param message подпояснение/текст
 * @param primaryActionTitle текст основной кнопки (опционально)
 * @param onPrimaryAction клик по основной кнопке
 * @param secondaryActionTitle текст вторичной кнопки (опционально)
 * @param onSecondaryAction клик по вторичной кнопке
 * @param contentPadding внешние отступы контейнера
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String,
    message: String? = null,
    primaryActionTitle: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionTitle: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        if (primaryActionTitle != null && onPrimaryAction != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onPrimaryAction) {
                Text(primaryActionTitle)
            }
        }

        if (secondaryActionTitle != null && onSecondaryAction != null) {
            OutlinedButton(onClick = onSecondaryAction) {
                Text(secondaryActionTitle)
            }
        }
    }
}