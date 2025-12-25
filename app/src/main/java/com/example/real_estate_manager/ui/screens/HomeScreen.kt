package com.example.real_estate_manager.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.real_estate_manager.data.model.TxType
import com.example.real_estate_manager.ui.AuthViewModel
import com.example.real_estate_manager.ui.RealEstateViewModel
import com.example.real_estate_manager.ui.util.moneyFormatPlain
import com.example.real_estate_manager.ui.util.monthName
import java.time.LocalDate

@Composable
fun HomeScreen(
    onOpenStats: () -> Unit,
    onOpenProperties: () -> Unit,
    onLogoutNavigate: () -> Unit = {}
) {
    val vm: RealEstateViewModel = hiltViewModel()
    val authVm: AuthViewModel = hiltViewModel()

    val transactions by vm.transactions.collectAsState()
    val properties by vm.properties.collectAsState()
    val currentEmail by authVm.currentEmail.collectAsState()

    val now = LocalDate.now()
    val mName = monthName(now.monthValue)
    val year = now.year

    // ⚙️ ЛОГИКА: будущие даты не учитываем (как зафиксировали)
    val monthTransactions = transactions.filter {
        it.date.year == year &&
                it.date.monthValue == now.monthValue &&
                !it.date.isAfter(now)
    }

    val income = monthTransactions.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val expense = monthTransactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val delta = income - expense

    // Анимация «дыхания» иконки
    val infiniteTransition = rememberInfiniteTransition(label = "home_breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )
    val cardAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_alpha"
    )

    // ✅ ТВОЯ анимация «города» внизу
    val citylinePhase by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cityline_phase"
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок + иконка пользователя
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Моя недвижимость",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        val propsCount = properties.size
                        val subtitle = when {
                            propsCount == 0 -> "Добавьте первый объект, чтобы начать учёт"
                            propsCount == 1 -> "1 объект в управлении"
                            else -> "$propsCount объектов в управлении"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    UserMenuButton(
                        email = currentEmail,
                        onLogout = {
                            authVm.logout()
                            onLogoutNavigate()
                        },
                        onChangePassword = { oldPwd, newPwd, onDone ->
                            authVm.changePassword(oldPwd, newPwd, onDone)
                        }
                    )
                }

                // Мини-виджет: итоги за текущий месяц
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .alpha(cardAlpha),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.PieChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column {
                                Text(
                                    text = "Итого за $mName $year",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Доход, расход и чистый результат за месяц",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Доход")
                                Text(
                                    text = moneyFormatPlain(income),
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Расход")
                                Text(
                                    text = "- ${moneyFormatPlain(expense)}",
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Чистый результат")
                                Text(
                                    text = moneyFormatPlain(delta),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Кнопки-действия
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeActionCard(
                        title = "Объекты недвижимости",
                        subtitle = "Список помещений, карточки объектов, счета и транзакции",
                        icon = Icons.Filled.Apartment,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenProperties
                    )

                    HomeActionCard(
                        title = "Статистика",
                        subtitle = "Общая картина: помесячно, по объектам и по типам транзакций",
                        icon = Icons.Filled.Assessment,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenStats
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ✅ ТВОЯ декоративная линия «города» (домики/окна)
                BottomCityline(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    phase = citylinePhase,
                    scroll = scrollState.value
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Управление недвижимостью",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
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
private fun UserMenuButton(
    email: String?,
    onLogout: () -> Unit,
    onChangePassword: (String, String, (String?) -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showChangePwd by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { expanded = true }) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Пользователь",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = email?.takeIf { it.isNotBlank() } ?: "Пользователь",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = { }
            )

            DropdownMenuItem(
                text = { Text("Сменить пароль") },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.LockReset,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    showChangePwd = true
                }
            )

            DropdownMenuItem(
                text = { Text("Выйти") },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }

    if (showChangePwd) {
        ChangePasswordDialog(
            onDismiss = { showChangePwd = false },
            onSave = { oldPwd, newPwd, done ->
                onChangePassword(oldPwd, newPwd, done)
            }
        )
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, (String?) -> Unit) -> Unit
) {
    var oldPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Сменить пароль") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = oldPwd,
                    onValueChange = { oldPwd = it; error = null },
                    label = { Text("Текущий пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = newPwd,
                    onValueChange = { newPwd = it; error = null },
                    label = { Text("Новый пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (saving) return@TextButton
                    saving = true
                    onSave(oldPwd, newPwd) { err ->
                        saving = false
                        if (err == null) onDismiss() else error = err
                    }
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = { if (!saving) onDismiss() }) { Text("Отмена") }
        }
    )
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = tint.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------------
// ✅ ТВОЙ КОД АНИМАЦИИ (домики/окна) — без изменений
// ---------------------
@Composable
private fun BottomCityline(
    modifier: Modifier = Modifier,
    phase: Float,
    scroll: Int
) {
    // Базовые высоты «домиков»
    val baseHeights = listOf(22, 34, 28, 42, 26, 38, 24)

    Row(
        modifier = modifier
            .graphicsLayer(
                // лёгкий параллакс: чем больше скролл, тем чуть выше/ниже «город»
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
                // «Огоньки» / окошки
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
