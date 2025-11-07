package com.example.my_project.ui.screens

import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Общие модели/утилиты для экранов статистики. */

data class Totals(
    val income: Double = 0.0,
    val expense: Double = 0.0
) {
    val total: Double get() = income - expense
}

data class MonthRow(
    val month: Int,
    val totals: Totals
)

/** Формат даты дд.ММ.гггг */
val DateFmtDMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Подсчёт итогов по списку транзакций. */
fun List<Transaction>.computeTotals(): Totals {
    var income = 0.0
    var expense = 0.0
    for (t in this) {
        if (t.type == TxType.INCOME) income += t.amount else expense += t.amount
    }
    return Totals(income, expense)
}

/** Локализованное название месяца (Рус), с заглавной буквы. */
fun monthName(m: Int): String {
    val ru = Locale("ru")
    return LocalDate.of(2000, m, 1).month
        .getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ru)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ru) else it.toString() }
}