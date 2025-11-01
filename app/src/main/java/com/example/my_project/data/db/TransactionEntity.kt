package com.example.my_project.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Транзакции по объекту. Дата хранится как ISO-строка (yyyy-MM-dd).
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String,
    /** true = INCOME, false = EXPENSE */
    val isIncome: Boolean,
    val amount: Double,
    /** ISO дата: yyyy-MM-dd */
    val dateIso: String,
    val note: String?
)