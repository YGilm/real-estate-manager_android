package com.example.real_estate_manager.data.model

import java.time.LocalDate
import java.util.Locale
import java.util.UUID

enum class TxType { INCOME, EXPENSE }

enum class TransactionSyncStatus { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE }

/**
 * Доменная модель транзакции.
 *
 * - id: UUID как строка
 * - propertyId: строковый id объекта
 * - type: INCOME / EXPENSE
 * - amount: сумма в деньгах
 * - date: дата операции
 * - note: комментарий
 * - attachment*: опциональный файл (счёт/чек)
 */
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val propertyId: String,
    val type: TxType,
    val amount: Double,
    val date: LocalDate,
    val note: String? = null,

    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val attachmentMime: String? = null,
    val syncStatus: TransactionSyncStatus = TransactionSyncStatus.SYNCED,
    val lastSyncError: String? = null,
    val lastSyncAttemptAt: Long? = null
)

/**
 * Форматирует сумму с учётом типа:
 *
 * INCOME  → "+1234.56"
 * EXPENSE → "-1234.56"
 *
 * По умолчанию — русский Locale и 2 знака после запятой.
 */
fun Transaction.signedAmountString(
    locale: Locale = Locale("ru", "RU")
): String {
    val sign = if (type == TxType.INCOME) "+" else "-"
    val numeric = String.format(locale, "%.2f", amount)
    return sign + numeric
}
