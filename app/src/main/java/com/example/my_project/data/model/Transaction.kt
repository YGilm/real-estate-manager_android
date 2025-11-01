package com.example.my_project.data.model

import java.time.LocalDate
import java.util.UUID

enum class TxType { INCOME, EXPENSE }

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val propertyId: String,
    val type: TxType,
    val amount: Double,
    val date: LocalDate,
    val note: String? = null
)