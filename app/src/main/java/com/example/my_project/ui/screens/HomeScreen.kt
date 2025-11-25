package com.example.my_project.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Assessment
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.my_project.data.model.TxType
import com.example.my_project.ui.RealEstateViewModel
import com.example.my_project.ui.util.moneyFormatPlain
import com.example.my_project.ui.util.monthName
import java.time.LocalDate

@Composable
fun HomeScreen(
    onOpenStats: () -> Unit,
    onOpenProperties: () -> Unit
) {
    val vm: RealEstateViewModel = hiltViewModel()

    val transactions by vm.transactions.collectAsState()
    val properties by vm.properties.collectAsState()

    val now = LocalDate.now()
    val monthName = monthName(now.monthValue)
    val year = now.year

    val monthTransactions = transactions.filter {
        it.date.year == year && it.date.monthValue == now.monthValue
    }

    val income = monthTransactions
        .filter { it.type == TxType.INCOME }
        .sumOf { it.amount }

    val expense = monthTransactions
        .filter { it.type == TxType.EXPENSE }
        .sumOf { it.amount }

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
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Заголовок
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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

                // Мини-виджет: итоги за текущий месяц
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .alpha(alpha),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale
                                    )
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
                                    text = "Итого за $monthName $year",
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
//                                Text(
//                                    text = moneyFormatPlain(expense),
//                                    color = Color(0xFFC62828),
//                                    fontWeight = FontWeight.SemiBold
//                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Чистый результат")
                                Text(
                                    text = moneyFormatPlain(delta),
                                    color = MaterialTheme.colorScheme.onSurface, // чёрный/основной текстовый
                                    fontWeight = FontWeight.SemiBold
                                )
//                                Text(
//                                    text = moneyFormatPlain(delta),
//                                    color = when {
//                                        delta > 0 -> Color(0xFF2E7D32)
//                                        delta < 0 -> Color(0xFFC62828)
//                                        else -> MaterialTheme.colorScheme.onSurface
//                                    },
//                                    fontWeight = FontWeight.SemiBold
//                                )
                            }
                        }
                    }
                }

                // Кнопки-действия
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Объекты
                    HomeActionCard(
                        title = "Объекты недвижимости",
                        subtitle = "Список помещений, карточки объектов, счета и транзакции",
                        icon = Icons.Filled.Apartment,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenProperties
                    )

                    // Статистика
                    HomeActionCard(
                        title = "Статистика",
                        subtitle = "Общая картина: помесячно, по объектам и по типам транзакций",
                        icon = Icons.Filled.Assessment,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenStats
                    )
//                    HomeActionCard(
//                        title = "Статистика",
//                        subtitle = "Общая картина: помесячно, по объектам и по типам транзакций",
//                        icon = Icons.Filled.PieChart,
//                        tint = MaterialTheme.colorScheme.secondary,
//                        onClick = onOpenStats
//                    )
                }
            }
        }
    }
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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