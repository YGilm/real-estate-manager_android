package com.example.my_project.data

import com.example.my_project.data.db.AttachmentDao
import com.example.my_project.data.db.AttachmentEntity
import com.example.my_project.data.db.PropertyDao
import com.example.my_project.data.db.PropertyEntity
import com.example.my_project.data.db.TransactionDao
import com.example.my_project.data.db.TransactionEntity
import com.example.my_project.data.model.Attachment
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRealEstateRepository @Inject constructor(
    private val propertyDao: PropertyDao,
    private val transactionDao: TransactionDao,
    private val attachmentDao: AttachmentDao
) : RealEstateRepository {

    // ---------- Properties ----------
    override fun properties(userId: String): Flow<List<Property>> =
        propertyDao.list(userId).map { list ->
            list.map {
                Property(
                    id = it.id,
                    name = it.name,
                    address = it.address,
                    monthlyRent = it.monthlyRent,
                    coverUri = it.coverUri
                )
            }
        }

    override suspend fun addProperty(userId: String, property: Property) =
        withContext(Dispatchers.IO) {
            val id = if (property.id.isBlank()) UUID.randomUUID().toString() else property.id
            propertyDao.upsert(
                PropertyEntity(
                    id = id,
                    userId = userId,
                    name = property.name,
                    address = property.address,
                    monthlyRent = property.monthlyRent,
                    coverUri = property.coverUri
                )
            )
        }

    override suspend fun getProperty(userId: String, id: String): Property? =
        withContext(Dispatchers.IO) {
            propertyDao.getById(userId, id)?.let {
                Property(
                    id = it.id,
                    name = it.name,
                    address = it.address,
                    monthlyRent = it.monthlyRent,
                    coverUri = it.coverUri
                )
            }
        }

    override suspend fun updateProperty(
        userId: String,
        id: String,
        name: String,
        address: String?,
        monthlyRent: Double?
    ) = withContext(Dispatchers.IO) {
        val existing = propertyDao.getById(userId, id) ?: return@withContext
        propertyDao.upsert(
            existing.copy(
                name = name,
                address = address,
                monthlyRent = monthlyRent
            )
        )
    }

    override suspend fun deletePropertyWithRelations(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            attachmentDao.deleteForProperty(userId, id)
            transactionDao.deleteForProperty(userId, id)
            propertyDao.delete(userId, id)
        }

    override suspend fun setPropertyCover(userId: String, propertyId: String, coverUri: String?) =
        withContext(Dispatchers.IO) {
            propertyDao.updateCover(userId, propertyId, coverUri)
        }

    // ---------- Transactions ----------
    override fun transactions(userId: String): Flow<List<Transaction>> =
        transactionDao.listAll(userId).map { list ->
            list.map {
                Transaction(
                    id = it.id,
                    propertyId = it.propertyId,
                    type = if (it.isIncome) TxType.INCOME else TxType.EXPENSE,
                    amount = it.amount,
                    date = LocalDate.parse(it.dateIso),
                    note = it.note
                )
            }
        }

    override suspend fun transactionsFor(userId: String, propertyId: String): List<Transaction> =
        withContext(Dispatchers.IO) {
            transactionDao.listForProperty(userId, propertyId).map {
                Transaction(
                    id = it.id,
                    propertyId = it.propertyId,
                    type = if (it.isIncome) TxType.INCOME else TxType.EXPENSE,
                    amount = it.amount,
                    date = LocalDate.parse(it.dateIso),
                    note = it.note
                )
            }
        }

    override suspend fun addTransaction(userId: String, tx: Transaction) =
        withContext(Dispatchers.IO) {
            val id = if (tx.id.isBlank()) UUID.randomUUID().toString() else tx.id
            transactionDao.upsert(
                TransactionEntity(
                    id = id,
                    userId = userId,
                    propertyId = tx.propertyId,
                    isIncome = (tx.type == TxType.INCOME),
                    amount = tx.amount,
                    dateIso = tx.date.toString(),
                    note = tx.note
                )
            )
        }

    override suspend fun updateTransaction(
        userId: String,
        id: String,
        type: TxType,
        amount: Double,
        date: LocalDate,
        note: String?
    ) = withContext(Dispatchers.IO) {
        val existing = transactionDao.getById(userId, id) ?: return@withContext
        transactionDao.upsert(
            existing.copy(
                isIncome = (type == TxType.INCOME),
                amount = amount,
                dateIso = date.toString(),
                note = note
            )
        )
    }

    override suspend fun deleteTransaction(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            transactionDao.delete(userId, id)
        }

    // ---------- Attachments ----------
    override suspend fun listAttachments(userId: String, propertyId: String): List<Attachment> =
        withContext(Dispatchers.IO) {
            val entities = attachmentDao.listForProperty(userId, propertyId)
            entities.map { e ->
                // ВАЖНО: доменная модель Attachment = (id, name, mimeType, uri)
                Attachment(
                    e.id,
                    e.name,
                    e.mimeType,
                    e.uri
                )
            }
        }

    override suspend fun addAttachment(
        userId: String,
        propertyId: String,
        name: String?,
        mime: String?,
        uri: String
    ) = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        // Позиционные аргументы под Room-сущность:
        // AttachmentEntity(id, userId, propertyId, name, mimeType, uri)
        val entity = AttachmentEntity(
            id,
            userId,
            propertyId,
            name,
            mime,
            uri
        )
        attachmentDao.upsert(entity)
    }

    override suspend fun deleteAttachment(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            attachmentDao.delete(userId, id)
        }
}