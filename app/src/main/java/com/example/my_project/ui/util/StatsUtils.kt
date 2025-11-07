package com.example.my_project.ui.util

import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// ЕДИНСТВЕННОЕ место, где объявлены эти утилиты

// Формат даты дд.мм.гггг
val DateFmtDMY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

data class Totals(
    val income: Double = 0.0,
    val expense: Double = 0.0
) {
    val total: Double get() = income - expense
}

fun List<Transaction>.computeTotals(): Totals {
    var income = 0.0
    var expense = 0.0
    for (t in this) {
        if (t.type == TxType.INCOME) income += t.amount else expense += t.amount
    }
    return Totals(income, expense)
}

fun monthName(m: Int): String {
    val ru = Locale("ru")
    return LocalDate.of(2000, m, 1).month
        .getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, ru)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ru) else it.toString() }
}

// Число без знака, 2 знака после запятой, локаль RU
fun moneyFormatPlain(v: Double): String {
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return nf.format(v)
}

// Строка суммы с учётом типа транзакции (расход — со знаком минус)
fun moneyFormat(v: Double, type: TxType): String {
    val nf = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    val core = nf.format(abs(v))
    return if (type == TxType.EXPENSE) "-$core" else core
}