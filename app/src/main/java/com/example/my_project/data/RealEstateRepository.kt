package com.example.my_project.data

import com.example.my_project.data.model.Attachment
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Центральный репозиторий домена недвижимости.
 *
 * Здесь собраны все операции:
 * - с объектами (properties),
 * - с транзакциями (transactions),
 * - с вложениями/файлами (attachments).
 *
 * Реализация: [RoomRealEstateRepository].
 */
interface RealEstateRepository {

    // ---------- Properties ----------

    /**
     * Реактивный поток всех объектов пользователя.
     */
    fun properties(userId: String): Flow<List<Property>>

    /**
     * Добавить / сохранить объект.
     * Если у [property.id] пустой id — реализация сама генерирует UUID.
     */
    suspend fun addProperty(userId: String, property: Property)

    /**
     * Получить один объект по id (или null, если не найден).
     */
    suspend fun getProperty(userId: String, id: String): Property?

    /**
     * Обновить основные поля объекта.
     */
    suspend fun updateProperty(
        userId: String,
        id: String,
        name: String,
        address: String?,
        monthlyRent: Double?,
        leaseFrom: String?,
        leaseTo: String?
    )

    /**
     * Удалить объект вместе со всеми связанными сущностями:
     * - транзакциями,
     * - вложениями.
     */
    suspend fun deletePropertyWithRelations(userId: String, id: String)

    /**
     * Обновить обложку (cover) объекта.
     */
    suspend fun setPropertyCover(userId: String, propertyId: String, coverUri: String?)

    // ---------- Transactions ----------

    /**
     * Реактивный поток всех транзакций пользователя
     * (не только по одному объекту).
     */
    fun transactions(userId: String): Flow<List<Transaction>>

    /**
     * Список транзакций по конкретному объекту.
     */
    suspend fun transactionsFor(
        userId: String,
        propertyId: String
    ): List<Transaction>

    /**
     * Добавить транзакцию.
     * Если у [transaction.id] пустой id — реализация сама генерирует UUID.
     */
    suspend fun addTransaction(userId: String, transaction: Transaction)

    /**
     * Обновить существующую транзакцию.
     */
    suspend fun updateTransaction(
        userId: String,
        id: String,
        type: TxType,
        amount: Double,
        date: LocalDate,
        note: String?
    )

    /**
     * Удалить транзакцию по id.
     */
    suspend fun deleteTransaction(userId: String, id: String)

    // ---------- Attachments ----------

    /**
     * Список вложений по объекту.
     */
    suspend fun listAttachments(userId: String, propertyId: String): List<Attachment>

    /**
     * Добавить вложение к объекту.
     */
    suspend fun addAttachment(
        userId: String,
        propertyId: String,
        name: String?,
        mime: String?,
        uri: String
    )

    /**
     * Удалить вложение.
     */
    suspend fun deleteAttachment(userId: String, id: String)
}