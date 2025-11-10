package com.example.my_project.ui.bills

import java.time.LocalDate
import java.util.UUID

/**
 * UI-модель счёта (пока без связи с БД).
 */
data class BillUi(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val dueDate: LocalDate,
    val isPaid: Boolean = false,
    val propertyName: String? = null, // опционально: к какой недвижимости относится
)