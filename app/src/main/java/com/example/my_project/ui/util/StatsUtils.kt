package com.example.my_project.ui.util

import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// Формат даты дд.мм.гггг
val DateFmtDMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/**
 * Итоги по деньгам: доход, расход и вычисляемый итог (доход - расход).
 */
data class Totals(
    val income: Double = 0.0,
    val expense: Double = 0.0
) {
    val total: Double get() = income - expense
}

/**
 * Вернуть только те транзакции, дата которых уже наступила (date <= сегодня).
 * Будущие транзакции считаются "запланированными" и в статистику не попадают.
 */
fun List<Transaction>.pastOnly(): List<Transaction> {
    val today = LocalDate.now()
    return this.filter { !it.date.isAfter(today) }
}

/**
 * Посчитать общие итоги по списку транзакций.
 * ВАЖНО: транзакции с датой в будущем игнорируются.
 */
fun List<Transaction>.computeTotals(): Totals {
    var income = 0.0
    var expense = 0.0

    for (t in this.pastOnly()) {
        if (t.type == TxType.INCOME) {
            income += t.amount
        } else {
            expense += t.amount
        }
    }

    return Totals(income = income, expense = expense)
}

/**
 * Локализованное название месяца (русский, с заглавной буквы).
 */
fun monthName(m: Int): String {
    val ru = Locale("ru")
    return LocalDate.of(2000, m, 1).month
        .getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ru)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ru) else it.toString() }
}

/**
 * Форматирование суммы без учёта типа, просто как число с 2 знаками.
 */
fun moneyFormatPlain(v: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return nf.format(v)
}

/**
 * Строка суммы с учётом типа транзакции:
 *  - для расхода значение всегда показывается со знаком минус,
 *  - для дохода — без знака минус.
 */
fun moneyFormat(v: Double, type: TxType): String {
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val core = nf.format(abs(v))
    return if (type == TxType.EXPENSE) "-$core" else core
}