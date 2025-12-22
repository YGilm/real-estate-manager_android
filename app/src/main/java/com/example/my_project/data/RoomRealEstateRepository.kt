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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация [RealEstateRepository] поверх Room-DAO.
 *
 * Все тяжёлые операции выполняются на Dispatchers.IO.
 */
@Singleton
class RoomRealEstateRepository @Inject constructor(
    private val propertyDao: PropertyDao,
    private val propertyDetailsDao: PropertyDetailsDao,
    private val propertyPhotoDao: PropertyPhotoDao,
    private val transactionDao: TransactionDao,
    private val attachmentDao: AttachmentDao,
) : RealEstateRepository {

    // ---------- Properties ----------

    override fun properties(userId: String): Flow<List<Property>> =
        propertyDao.list(userId).map { list ->
            list.map { it.toModel() }
        }

    override suspend fun addProperty(userId: String, property: Property) =
        withContext(Dispatchers.IO) {
            val entity = property.toEntity(userId)
            propertyDao.upsert(entity)
        }

    override suspend fun getProperty(userId: String, id: String): Property? =
        withContext(Dispatchers.IO) {
            propertyDao.getById(userId, id)?.toModel()
        }

    override suspend fun updateProperty(
        userId: String,
        id: String,
        name: String,
        address: String?,
        monthlyRent: Double?,
        leaseFrom: String?,
        leaseTo: String?,
    ) = withContext(Dispatchers.IO) {
        val current = propertyDao.getById(userId, id) ?: return@withContext
        val updated = current.copy(
            name = name,
            address = address,
            monthlyRent = monthlyRent,
            leaseFrom = leaseFrom,
            leaseTo = leaseTo
        )
        propertyDao.upsert(updated)
    }

    override suspend fun deletePropertyWithRelations(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            // сначала дочерние сущности, затем сам объект
            transactionDao.deleteForProperty(userId, id)
            attachmentDao.deleteForProperty(userId, id)
            propertyDetailsDao.deleteForProperty(userId, id)
            propertyPhotoDao.deleteForProperty(userId, id)
            propertyDao.delete(userId, id)
        }

    override suspend fun setPropertyCover(
        userId: String,
        propertyId: String,
        coverUri: String?,
    ) = withContext(Dispatchers.IO) {
        propertyDao.updateCover(userId, propertyId, coverUri)
    }

    // ---------- Property details ----------

    override fun propertyDetails(userId: String, propertyId: String): Flow<PropertyDetails?> =
        propertyDetailsDao.observe(userId, propertyId).map { entity ->
            entity?.toModel()
        }

    override suspend fun upsertPropertyDetails(
        userId: String,
        propertyId: String,
        description: String?,
        areaSqm: String?,
    ) = withContext(Dispatchers.IO) {
        val entity = PropertyDetailsEntity(
            userId = userId,
            propertyId = propertyId,
            description = description,
            areaSqm = areaSqm,
            updatedAt = System.currentTimeMillis()
        )
        propertyDetailsDao.upsert(entity)
    }

    // ---------- Property photos ----------

    override fun propertyPhotos(userId: String, propertyId: String): Flow<List<PropertyPhoto>> =
        propertyPhotoDao.observeForProperty(userId, propertyId).map { list ->
            list.map { it.toModel() }
        }

    override suspend fun addPropertyPhotos(
        userId: String,
        propertyId: String,
        uris: List<String>,
    ) = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext

        val baseTime = System.currentTimeMillis()
        val entities = uris.mapIndexed { index, uri ->
            PropertyPhotoEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                propertyId = propertyId,
                uri = uri,
                // более новые фото — больше createdAt, они будут выше
                createdAt = baseTime + index
            )
        }
        propertyPhotoDao.upsertAll(entities)
    }

    override suspend fun deletePropertyPhoto(userId: String, photoId: String) =
        withContext(Dispatchers.IO) {
            propertyPhotoDao.delete(userId, photoId)
        }

    override suspend fun updatePropertyPhotoUri(userId: String, photoId: String, uri: String) =
        withContext(Dispatchers.IO) {
            propertyPhotoDao.updateUri(userId, photoId, uri)
        }

    override suspend fun reorderPropertyPhotos(
        userId: String,
        propertyId: String,
        orderedIds: List<String>,
    ) = withContext(Dispatchers.IO) {
        if (orderedIds.isEmpty()) return@withContext

        val current = propertyPhotoDao
            .observeForProperty(userId, propertyId)
            .first()

        if (current.isEmpty()) return@withContext

        val byId = current.associateBy { it.id }
        val orderedSet = orderedIds.toSet()

        val ordered = orderedIds.mapNotNull { byId[it] }
        val tail = current.filter { it.id !in orderedSet }

        val now = System.currentTimeMillis()
        var offset = (ordered.size + tail.size).toLong()

        val newList = (ordered + tail).map { entity ->
            entity.copy(createdAt = now + offset--)
        }

        propertyPhotoDao.upsertAll(newList)
    }

    // ---------- Transactions ----------

    override fun transactions(userId: String): Flow<List<Transaction>> =
        transactionDao.listAll(userId).map { list ->
            list.map { it.toModel() }
        }

    override suspend fun transactionsFor(
        userId: String,
        propertyId: String,
    ): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.listForProperty(userId, propertyId).map { it.toModel() }
    }

    override suspend fun addTransaction(
        userId: String,
        propertyId: String,
        type: TxType,
        amount: Double,
        date: LocalDate,
        note: String?,
        attachmentUri: String?,
        attachmentName: String?,
        attachmentMime: String?
    ) = withContext(Dispatchers.IO) {
        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            propertyId = propertyId,
            isIncome = type == TxType.INCOME,
            amount = amount,
            dateIso = date.toString(),
            note = note,
            attachmentUri = attachmentUri,
            attachmentName = attachmentName,
            attachmentMime = attachmentMime
        )
        transactionDao.upsert(entity)
    }

    override suspend fun updateTransaction(
        userId: String,
        id: String,
        type: TxType,
        amount: Double,
        date: LocalDate,
        note: String?,
        attachmentUri: String?,
        attachmentName: String?,
        attachmentMime: String?
    ) = withContext(Dispatchers.IO) {
        val existing = transactionDao.getById(userId, id) ?: return@withContext
        val entity = existing.copy(
            isIncome = type == TxType.INCOME,
            amount = amount,
            dateIso = date.toString(),
            note = note,
            attachmentUri = attachmentUri,
            attachmentName = attachmentName,
            attachmentMime = attachmentMime
        )
        transactionDao.upsert(entity)
    }

    override suspend fun deleteTransaction(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            transactionDao.delete(userId, id)
        }

    // ---------- Attachments (documents) ----------

    override fun attachments(userId: String, propertyId: String): Flow<List<Attachment>> =
        attachmentDao.observeForProperty(userId, propertyId).map { list ->
            list.map { it.toModel() }
        }

    override suspend fun listAttachments(
        userId: String,
        propertyId: String,
    ): List<Attachment> = withContext(Dispatchers.IO) {
        attachmentDao.listForProperty(userId, propertyId).map { it.toModel() }
    }

    override suspend fun addAttachment(
        userId: String,
        propertyId: String,
        name: String?,
        mime: String?,
        uri: String,
    ) = withContext(Dispatchers.IO) {
        val entity = AttachmentEntity(
            id = UUID.randomUUID().toString(),
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

// ---------- Маппинги сущностей ----------

private fun PropertyEntity.toModel(): Property =
    Property(
        id = id,
        name = name,
        address = address,
        monthlyRent = monthlyRent,
        coverUri = coverUri,
        leaseFrom = leaseFrom,
        leaseTo = leaseTo
    )

private fun Property.toEntity(userId: String): PropertyEntity =
    PropertyEntity(
        id = id,
        userId = userId,
        name = name,
        address = address,
        monthlyRent = monthlyRent,
        coverUri = coverUri,
        leaseFrom = leaseFrom,
        leaseTo = leaseTo
    )

private fun PropertyDetailsEntity.toModel(): PropertyDetails =
    PropertyDetails(
        propertyId = propertyId,
        description = description,
        areaSqm = areaSqm
    )

private fun PropertyPhotoEntity.toModel(): PropertyPhoto =
    PropertyPhoto(
        id = id,
        propertyId = propertyId,
        uri = uri
    )

private fun TransactionEntity.toModel(): Transaction =
    Transaction(
        id = id,
        propertyId = propertyId,
        type = if (isIncome) TxType.INCOME else TxType.EXPENSE,
        amount = amount,
        date = LocalDate.parse(dateIso),
        note = note,
        attachmentUri = attachmentUri,
        attachmentName = attachmentName,
        attachmentMime = attachmentMime
    )

private fun AttachmentEntity.toModel(): Attachment =
    Attachment(
        id = id,
        propertyId = propertyId,
        name = name,
        mimeType = mimeType,
        uri = uri
    )
