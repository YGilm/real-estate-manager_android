package com.example.my_project.data

import com.example.my_project.data.db.AttachmentDao
import com.example.my_project.data.db.AttachmentEntity
import com.example.my_project.data.db.PropertyDao
import com.example.my_project.data.db.PropertyDetailsDao
import com.example.my_project.data.db.PropertyDetailsEntity
import com.example.my_project.data.db.PropertyEntity
import com.example.my_project.data.db.PropertyPhotoDao
import com.example.my_project.data.db.PropertyPhotoEntity
import com.example.my_project.data.db.TransactionDao
import com.example.my_project.data.db.TransactionEntity
import com.example.my_project.data.model.Attachment
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.PropertyDetails
import com.example.my_project.data.model.PropertyPhoto
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
    private val attachmentDao: AttachmentDao,
    private val propertyDetailsDao: PropertyDetailsDao,
    private val propertyPhotoDao: PropertyPhotoDao,
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
                    coverUri = it.coverUri,
                    leaseFrom = it.leaseFrom,
                    leaseTo = it.leaseTo
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
                    coverUri = property.coverUri,
                    leaseFrom = property.leaseFrom,
                    leaseTo = property.leaseTo
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
                    coverUri = it.coverUri,
                    leaseFrom = it.leaseFrom,
                    leaseTo = it.leaseTo
                )
            }
        }

    override suspend fun updateProperty(
        userId: String,
        id: String,
        name: String,
        address: String?,
        monthlyRent: Double?,
        leaseFrom: String?,
        leaseTo: String?
    ) = withContext(Dispatchers.IO) {
        val existing = propertyDao.getById(userId, id) ?: return@withContext
        propertyDao.upsert(
            existing.copy(
                name = name,
                address = address,
                monthlyRent = monthlyRent,
                leaseFrom = leaseFrom,
                leaseTo = leaseTo
            )
        )
    }

    override suspend fun deletePropertyWithRelations(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            // сначала дочерние данные
            attachmentDao.deleteForProperty(userId, id)
            transactionDao.deleteForProperty(userId, id)
            propertyPhotoDao.deleteForProperty(userId, id)
            propertyDetailsDao.deleteForProperty(userId, id)

            // затем сам объект
            propertyDao.delete(userId, id)
        }

    override suspend fun setPropertyCover(userId: String, propertyId: String, coverUri: String?) =
        withContext(Dispatchers.IO) {
            propertyDao.updateCover(userId, propertyId, coverUri)
        }

    // ---------- Property details ----------
    override fun propertyDetails(userId: String, propertyId: String): Flow<PropertyDetails?> =
        propertyDetailsDao.observe(userId, propertyId).map { e ->
            e?.let { PropertyDetails(propertyId = it.propertyId, description = it.description, areaSqm = it.areaSqm) }
        }

    override suspend fun upsertPropertyDetails(
        userId: String,
        propertyId: String,
        description: String?,
        areaSqm: String?
    ) = withContext(Dispatchers.IO) {
        propertyDetailsDao.upsert(
            PropertyDetailsEntity(
                userId = userId,
                propertyId = propertyId,
                description = description,
                areaSqm = areaSqm,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // ---------- Property photos ----------
    override fun propertyPhotos(userId: String, propertyId: String): Flow<List<PropertyPhoto>> =
        propertyPhotoDao.observeForProperty(userId, propertyId).map { list ->
            list.map { PropertyPhoto(id = it.id, propertyId = it.propertyId, uri = it.uri) }
        }

    override suspend fun addPropertyPhotos(userId: String, propertyId: String, uris: List<String>) =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty()) return@withContext
            val now = System.currentTimeMillis()
            val entities = uris.map { uri ->
                PropertyPhotoEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    propertyId = propertyId,
                    uri = uri,
                    createdAt = now
                )
            }
            propertyPhotoDao.upsertAll(entities)
        }

    override suspend fun deletePropertyPhoto(userId: String, photoId: String) =
        withContext(Dispatchers.IO) {
            propertyPhotoDao.delete(userId, photoId)
        }

    // ---------- Transactions ----------
    override fun transactions(userId: String): Flow<List<Transaction>> =
        transactionDao.listAll(userId).map { list ->
            list.map { e ->
                Transaction(
                    id = e.id,
                    propertyId = e.propertyId,
                    type = if (e.isIncome) TxType.INCOME else TxType.EXPENSE,
                    amount = e.amount,
                    date = LocalDate.parse(e.dateIso),
                    note = e.note
                )
            }
        }

    override suspend fun transactionsFor(userId: String, propertyId: String): List<Transaction> =
        withContext(Dispatchers.IO) {
            transactionDao.listForProperty(userId, propertyId).map { e ->
                Transaction(
                    id = e.id,
                    propertyId = e.propertyId,
                    type = if (e.isIncome) TxType.INCOME else TxType.EXPENSE,
                    amount = e.amount,
                    date = LocalDate.parse(e.dateIso),
                    note = e.note
                )
            }
        }

    override suspend fun addTransaction(
        userId: String,
        propertyId: String,
        type: TxType,
        amount: Double,
        date: LocalDate,
        note: String?
    ) = withContext(Dispatchers.IO) {
        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            propertyId = propertyId,
            isIncome = type == TxType.INCOME,
            amount = amount,
            dateIso = date.toString(),
            note = note
        )
        transactionDao.upsert(entity)
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
                isIncome = type == TxType.INCOME,
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
    override fun attachments(userId: String, propertyId: String): Flow<List<Attachment>> =
        attachmentDao.observeForProperty(userId, propertyId).map { list ->
            list.map { e ->
                Attachment(
                    id = e.id,
                    propertyId = e.propertyId,
                    name = e.name,
                    mimeType = e.mimeType,
                    uri = e.uri
                )
            }
        }

    override suspend fun listAttachments(userId: String, propertyId: String): List<Attachment> =
        withContext(Dispatchers.IO) {
            attachmentDao.listForProperty(userId, propertyId).map { e ->
                Attachment(
                    id = e.id,
                    propertyId = e.propertyId,
                    name = e.name,
                    mimeType = e.mimeType,
                    uri = e.uri
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
        val entity = AttachmentEntity(
            id = id,
            userId = userId,
            propertyId = propertyId,
            name = name,
            mimeType = mime,
            uri = uri
        )
        attachmentDao.upsert(entity)
    }

    override suspend fun deleteAttachment(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            attachmentDao.delete(userId, id)
        }
}