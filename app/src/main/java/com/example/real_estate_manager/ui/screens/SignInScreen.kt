package com.example.real_estate_manager.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SignInScreen(
    onSignIn: (email: String, password: String, remember: Boolean, onDone: (String?) -> Unit) -> Unit,
    onGoSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "signin_anim")

    // “дыхание” иконки
    val iconScale by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signin_icon_scale"
    )
    val cardAlpha by infinite.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signin_card_alpha"
    )

    // “город” снизу
    val citylinePhase by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signin_cityline_phase"
    )

    val scrollState = rememberScrollState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            // контент (скролл на маленьких экранах)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Добро пожаловать",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Войдите, чтобы продолжить работу с объектами",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(Modifier.height(18.dp))

                Card(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .alpha(cardAlpha),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "Вход",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; error = null },
                            label = { Text("Email") },
                            singleLine = true,
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )

                        OutlinedTextField(
                            value = pass,
                            onValueChange = { pass = it; error = null },
                            label = { Text("Пароль") },
                            singleLine = true,
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPassword = !showPassword },
                                    enabled = !loading
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = if (showPassword) {
                                            "Скрыть пароль"
                                        } else {
                                            "Показать пароль"
                                        }
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = rememberMe,
                                    enabled = !loading,
                                    onValueChange = { rememberMe = it }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = null,
                                enabled = !loading
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Запомнить меня",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (error != null) {
                            Text(
                                text = error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = {
                                if (loading) return@Button
                                loading = true
                                error = null
                                onSignIn(email, pass, rememberMe) { err ->
                                    loading = false
                                    error = err
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Входим...")
                            } else {
                                Text("Войти")
                            }
                        }

                        TextButton(
                            onClick = onGoSignUp,
                            enabled = !loading,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Создать аккаунт")
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // “город” снизу — как на Home
                BottomCityline(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    phase = citylinePhase,
                    scroll = scrollState.value
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Управление недвижимостью",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BottomCityline(
    modifier: Modifier = Modifier,
    phase: Float,
    scroll: Int
) {
    val baseHeights = listOf(22, 34, 28, 42, 26, 38, 24)

    Row(
        modifier = modifier.graphicsLayer(
            translationY = -(scroll * 0.05f)
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        baseHeights.forEachIndexed { index, h ->
            val animatedFactor = 1.0f + (0.0f * phase)
            val height = (h * animatedFactor).dp

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rows = 2
                    repeat(rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 6.dp, height = 8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Color(0xFFFFF9C4).copy(
                                                alpha = if ((index + it + rows) % 2 == 0) 0.85f else 0.35f
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
