package com.example.real_estate_manager.data

import android.util.Log
import com.example.real_estate_manager.data.db.CustomProviderFieldEntity
import com.example.real_estate_manager.data.db.MeterReadingEntity
import com.example.real_estate_manager.data.db.PropertyDao
import com.example.real_estate_manager.data.db.CustomProviderFieldDao
import com.example.real_estate_manager.data.db.MeterReadingDao
import com.example.real_estate_manager.data.db.PropertyDocumentDao
import com.example.real_estate_manager.data.db.PropertyDetailsDao
import com.example.real_estate_manager.data.db.PropertyDetailsEntity
import com.example.real_estate_manager.data.db.PropertyPhotoDao
import com.example.real_estate_manager.data.db.TransactionDao
import com.example.real_estate_manager.data.db.TransactionEntity
import com.example.real_estate_manager.data.db.UtilityProviderEntity
import com.example.real_estate_manager.data.db.UtilityProviderDao
import com.example.real_estate_manager.data.model.Attachment
import com.example.real_estate_manager.data.model.FieldEntry
import com.example.real_estate_manager.data.model.Property
import com.example.real_estate_manager.data.model.PropertyDetails
import com.example.real_estate_manager.data.model.PropertyPhoto
import com.example.real_estate_manager.data.model.ProviderWidget
import com.example.real_estate_manager.data.model.ProviderWidgetType
import com.example.real_estate_manager.data.model.Transaction
import com.example.real_estate_manager.data.model.TxType
import com.example.real_estate_manager.data.model.WidgetField
import com.example.real_estate_manager.data.model.WidgetFieldType
import com.example.real_estate_manager.network.dto.CustomProviderFieldDto
import com.example.real_estate_manager.network.dto.MeterReadingDto
import com.example.real_estate_manager.network.dto.PropertyRequestDto
import com.example.real_estate_manager.network.dto.UtilityProviderDto
import com.example.real_estate_manager.network.mappers.toEntity
import com.example.real_estate_manager.network.mappers.toDetailsEntity
import com.example.real_estate_manager.network.mappers.toRequestDto
import com.example.real_estate_manager.network.mappers.transactionRequestDto
import com.example.real_estate_manager.network.remote.RemoteCustomProviderFieldDataSource
import com.example.real_estate_manager.network.remote.RemoteMeterReadingDataSource
import com.example.real_estate_manager.network.remote.RemotePropertyDocumentDataSource
import com.example.real_estate_manager.network.remote.RemotePropertyDataSource
import com.example.real_estate_manager.network.remote.RemotePropertyPhotoDataSource
import com.example.real_estate_manager.network.remote.RemoteTransactionDataSource
import com.example.real_estate_manager.network.remote.RemoteUtilityProviderDataSource
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudRealEstateRepository @Inject constructor(
    private val local: RoomRealEstateRepository,
    private val propertyDao: PropertyDao,
    private val propertyDetailsDao: PropertyDetailsDao,
    private val propertyPhotoDao: PropertyPhotoDao,
    private val propertyDocumentDao: PropertyDocumentDao,
    private val utilityProviderDao: UtilityProviderDao,
    private val customProviderFieldDao: CustomProviderFieldDao,
    private val meterReadingDao: MeterReadingDao,
    private val transactionDao: TransactionDao,
    private val remoteProperties: RemotePropertyDataSource,
    private val remoteTransactions: RemoteTransactionDataSource,
    private val remotePhotos: RemotePropertyPhotoDataSource,
    private val remoteDocuments: RemotePropertyDocumentDataSource,
    private val remoteProviders: RemoteUtilityProviderDataSource,
    private val remoteCustomFields: RemoteCustomProviderFieldDataSource,
    private val remoteReadings: RemoteMeterReadingDataSource,
    private val gson: Gson
) : RealEstateRepository {
    private companion object {
        const val TAG = "CloudSync"
        const val TARGET_PROPERTY = "Скандинавия центр"
    }

    private val aggregateSyncMutex = Mutex()

    override fun properties(userId: String): Flow<List<Property>> = flow {
        runCatching { syncProperties(userId) }
        emitAll(local.properties(userId))
    }

    override suspend fun addProperty(userId: String, property: Property) =
        withContext(Dispatchers.IO) {
            val created = remoteProperties.create(property.toRequestDto())
            propertyDao.upsert(created.toEntity(userId))
        }

    override suspend fun getProperty(userId: String, id: String): Property? =
        withContext(Dispatchers.IO) {
            runCatching { syncProperties(userId) }
            local.getProperty(userId, id)
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
        val current = local.getProperty(userId, id)
        val updated = Property(
            id = id,
            name = name,
            address = address,
            squareMeters = current?.squareMeters,
            monthlyRent = monthlyRent,
            pricePerM2 = current?.pricePerM2,
            coverUri = current?.coverUri,
            leaseFrom = leaseFrom,
            leaseTo = leaseTo
        )
        propertyDao.upsert(remoteProperties.update(id, updated.toRequestDto()).toEntity(userId))
    }

    override suspend fun deletePropertyWithRelations(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            remoteProperties.delete(id)
            local.deletePropertyWithRelations(userId, id)
        }

    override suspend fun setPropertyCover(userId: String, propertyId: String, coverUri: String?) =
        withContext(Dispatchers.IO) {
            val current = local.getProperty(userId, propertyId) ?: return@withContext
            val updated = current.copy(coverUri = coverUri)
            propertyDao.upsert(remoteProperties.update(propertyId, updated.toRequestDto()).toEntity(userId))
        }

    override fun transactions(userId: String): Flow<List<Transaction>> = flow {
        runCatching { syncTransactions(userId) }
        emitAll(local.transactions(userId))
    }

    override suspend fun transactionsFor(userId: String, propertyId: String): List<Transaction> =
        withContext(Dispatchers.IO) {
            runCatching { syncTransactions(userId, propertyId) }
            local.transactionsFor(userId, propertyId)
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
        val localId = UUID.randomUUID().toString()
        transactionDao.upsert(
            TransactionEntity(
                id = localId,
                userId = userId,
                propertyId = propertyId,
                isIncome = type == TxType.INCOME,
                amount = amount,
                dateIso = date.toString(),
                note = note,
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                attachmentMime = attachmentMime,
                syncStatus = SYNC_PENDING_CREATE
            )
        )
        syncPendingTransactions(userId)
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
        val nextStatus = if (existing.syncStatus == SYNC_PENDING_CREATE) {
            SYNC_PENDING_CREATE
        } else {
            SYNC_PENDING_UPDATE
        }
        transactionDao.upsert(
            existing.copy(
                isIncome = type == TxType.INCOME,
                amount = amount,
                dateIso = date.toString(),
                note = note,
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                attachmentMime = attachmentMime,
                syncStatus = nextStatus,
                lastSyncError = null
            )
        )
        syncPendingTransactions(userId)
    }

    override suspend fun deleteTransaction(userId: String, id: String) =
        withContext(Dispatchers.IO) {
            val existing = transactionDao.getById(userId, id) ?: return@withContext
            if (existing.syncStatus == SYNC_PENDING_CREATE) {
                transactionDao.delete(userId, id)
                return@withContext
            }
            transactionDao.upsert(
                existing.copy(
                    syncStatus = SYNC_PENDING_DELETE,
                    lastSyncError = null,
                    lastSyncAttemptAt = null
                )
            )
            syncPendingTransactions(userId)
        }

    private suspend fun syncProperties(userId: String) {
        syncPendingPropertyDetails(userId)
        val remote = remoteProperties.fetchAll()
        val entities = remote
            .filter { !it.id.isNullOrBlank() }
            .map { it.toEntity(userId) }
        val pendingDetailsIds = propertyDetailsDao.pending(userId).map { it.propertyId }.toSet()
        val details = remote
            .mapNotNull { it.toDetailsEntity(userId) }
            .filter { it.propertyId !in pendingDetailsIds }
        Log.d(TAG, "properties fetched=${remote.size}, cached=${entities.size}, details=${details.size}")
        if (entities.isEmpty()) propertyDao.deleteAll(userId) else {
            propertyDao.upsertAll(entities)
            propertyDao.deleteMissing(userId, entities.map { it.id })
        }
        if (details.isNotEmpty()) {
            details.forEach { propertyDetailsDao.upsert(it) }
        }
        entities.firstOrNull { it.name.equals(TARGET_PROPERTY, ignoreCase = true) }?.let {
            Log.d(
                TAG,
                "PropertyEntity target id=${it.id} squareMeters=${it.squareMeters} " +
                    "pricePerM2=${it.pricePerM2} monthlyRent=${it.monthlyRent} leaseFrom=${it.leaseFrom} leaseTo=${it.leaseTo}"
            )
        }
        entities.forEach { property ->
            syncPropertyAggregate(userId, property.id)
        }
    }

    private suspend fun syncPropertyAggregate(userId: String, propertyId: String) {
        runCatching {
            val photos = remotePhotos.fetchAll(propertyId)
                .mapNotNull { it.toEntity(userId) }
            if (photos.isEmpty()) propertyPhotoDao.deleteForProperty(userId, propertyId) else {
                propertyPhotoDao.upsertAll(photos)
                propertyPhotoDao.deleteMissingForProperty(userId, propertyId, photos.map { it.id })
            }
            Log.d(TAG, "property photos cached=${photos.size}, propertyId=$propertyId")
        }.onFailure { Log.w(TAG, "property photos sync failed propertyId=$propertyId", it) }

        runCatching {
            val documents = remoteDocuments.fetchAll(propertyId)
                .mapNotNull { it.toEntity(userId) }
            if (documents.isEmpty()) propertyDocumentDao.deleteForProperty(userId, propertyId) else {
                propertyDocumentDao.upsertAll(documents)
                propertyDocumentDao.deleteMissingForProperty(userId, propertyId, documents.map { it.id })
            }
            Log.d(TAG, "property documents cached=${documents.size}, propertyId=$propertyId")
        }.onFailure { Log.w(TAG, "property documents sync failed propertyId=$propertyId", it) }

        val providerIds = runCatching {
            syncPendingAggregate(userId)
            val providers = remoteProviders.fetchAll(propertyId)
                .mapNotNull { it.toEntity(userId, gson) }
            val pendingProviderIds = utilityProviderDao.pending(userId)
                .filter { it.propertyId == propertyId }
                .map { it.id }
                .toSet()
            val mergeProviders = providers.filter { it.id !in pendingProviderIds }
            if (mergeProviders.isEmpty()) utilityProviderDao.deleteForProperty(userId, propertyId) else {
                utilityProviderDao.upsertAll(mergeProviders)
                utilityProviderDao.deleteMissingForProperty(userId, propertyId, mergeProviders.map { it.id })
            }
            val cachedProviders = utilityProviderDao.listForProperty(userId, propertyId)
            Log.d(TAG, "utility providers cached=${mergeProviders.size}, propertyId=$propertyId")
            Log.d(TAG, "UtilityProviderEntity propertyId=$propertyId count=${cachedProviders.size} data=$cachedProviders")
            cachedProviders.map { it.id }
        }.onFailure { Log.w(TAG, "utility providers sync failed propertyId=$propertyId", it) }.getOrDefault(emptyList())

        providerIds.forEach { providerId ->
            runCatching {
                val pendingFieldIds = customProviderFieldDao.pending(userId)
                    .filter { it.providerId == providerId }
                    .map { it.id }
                    .toSet()
                val fields = remoteCustomFields.fetchAll(providerId)
                    .mapNotNull { it.toEntity(userId, gson) }
                    .filter { it.id !in pendingFieldIds }
                if (fields.isEmpty()) customProviderFieldDao.deleteForProvider(userId, providerId) else {
                    customProviderFieldDao.upsertAll(fields)
                    customProviderFieldDao.deleteMissingForProvider(userId, providerId, fields.map { it.id })
                }
                val cachedFields = customProviderFieldDao.listForProvider(userId, providerId)
                Log.d(TAG, "custom fields cached=${fields.size}, providerId=$providerId")
                Log.d(TAG, "CustomProviderFieldEntity providerId=$providerId count=${cachedFields.size} data=$cachedFields")
            }.onFailure { Log.w(TAG, "custom fields sync failed providerId=$providerId", it) }
        }

        runCatching {
            val pendingReadingKeys = meterReadingDao.pending(userId)
                .filter { it.propertyId == propertyId }
                .map { MeterReadingKey(it.providerId, it.periodYear, it.periodMonth) }
                .toSet()
            val readings = remoteReadings.fetchAll(propertyId = propertyId)
                .mapNotNull { it.toEntity(userId, gson) }
                .filter { MeterReadingKey(it.providerId, it.periodYear, it.periodMonth) !in pendingReadingKeys }
            if (readings.isEmpty()) meterReadingDao.deleteForProperty(userId, propertyId) else {
                meterReadingDao.upsertAll(readings)
                meterReadingDao.deleteMissingForProperty(userId, propertyId, readings.map { it.id })
            }
            val cachedReadings = meterReadingDao.listForProperty(userId, propertyId)
            Log.d(TAG, "meter readings cached=${readings.size}, propertyId=$propertyId")
            Log.d(TAG, "MeterReadingEntity propertyId=$propertyId count=${cachedReadings.size} data=$cachedReadings")
        }.onFailure { Log.w(TAG, "meter readings sync failed propertyId=$propertyId", it) }
    }

    private suspend fun syncTransactions(userId: String, propertyId: String? = null) {
        syncPendingTransactions(userId)
        val remote = remoteTransactions.fetchAll(propertyId)
        val entities = remote
            .map { it.toEntity(userId) }
            .filter { it.id.isNotBlank() && it.propertyId.isNotBlank() }
        Log.d(TAG, "transactions fetched=${remote.size}, cached=${entities.size}, propertyId=$propertyId")
        if (propertyId == null) {
            if (entities.isEmpty()) transactionDao.deleteAll(userId) else {
                transactionDao.upsertAll(entities)
                transactionDao.deleteMissing(userId, entities.map { it.id })
            }
        } else {
            if (entities.isNotEmpty()) transactionDao.upsertAll(entities)
        }
    }

    private suspend fun syncPendingTransactions(userId: String) {
        transactionDao.pending(userId).forEach { pending ->
            try {
                val now = System.currentTimeMillis()
                transactionDao.upsert(pending.copy(lastSyncAttemptAt = now, lastSyncError = null))
                when (pending.syncStatus) {
                    SYNC_PENDING_CREATE -> {
                        val created = remoteTransactions.create(pending.toRequestDto())
                        val remoteEntity = created.toEntity(userId)
                        if (remoteEntity.id.isBlank()) {
                            transactionDao.upsert(
                                pending.copy(lastSyncError = "Сервер вернул пустой id", lastSyncAttemptAt = now)
                            )
                        } else {
                            if (remoteEntity.id != pending.id) {
                                transactionDao.delete(userId, pending.id)
                            }
                            transactionDao.upsert(remoteEntity)
                        }
                    }
                    SYNC_PENDING_UPDATE -> {
                        val updated = remoteTransactions.update(pending.id, pending.toRequestDto())
                        transactionDao.upsert(updated.toEntity(userId))
                    }
                    SYNC_PENDING_DELETE -> {
                        remoteTransactions.delete(pending.id)
                        transactionDao.delete(userId, pending.id)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (pending.syncStatus == SYNC_PENDING_DELETE && error.isRemoteNotFound()) {
                    transactionDao.delete(userId, pending.id)
                    Log.d(TAG, "Transaction tombstone cleaned: transactionId=${pending.id}")
                    return@forEach
                }
                transactionDao.upsert(
                    pending.copy(
                        lastSyncError = error.toSyncMessage(),
                        lastSyncAttemptAt = System.currentTimeMillis()
                    )
                )
                Log.w(TAG, "transaction sync failed id=${pending.id} status=${pending.syncStatus}", error)
            }
        }
    }

    override fun propertyDetails(userId: String, propertyId: String): Flow<PropertyDetails?> = flow {
        runCatching { syncProperties(userId) }
        emitAll(local.propertyDetails(userId, propertyId))
    }

    override suspend fun upsertPropertyDetails(
        userId: String,
        propertyId: String,
        description: String?,
        areaSqm: String?
    ) = withContext(Dispatchers.IO) {
        val existing = propertyDetailsDao.getById(userId, propertyId)
        val nextStatus = if (existing?.syncStatus == SYNC_PENDING_CREATE) {
            SYNC_PENDING_CREATE
        } else if (existing == null) {
            SYNC_PENDING_CREATE
        } else {
            SYNC_PENDING_UPDATE
        }
        propertyDetailsDao.upsert(
            PropertyDetailsEntity(
                userId = userId,
                propertyId = propertyId,
                description = description,
                areaSqm = areaSqm,
                updatedAt = System.currentTimeMillis(),
                syncStatus = nextStatus,
                lastSyncError = null,
                lastSyncAttemptAt = null
            )
        )
        syncPendingPropertyDetails(userId)
    }

    override fun propertyPhotos(userId: String, propertyId: String): Flow<List<PropertyPhoto>> =
        local.propertyPhotos(userId, propertyId)

    override suspend fun addPropertyPhotos(userId: String, propertyId: String, uris: List<String>) =
        local.addPropertyPhotos(userId, propertyId, uris)

    override suspend fun deletePropertyPhoto(userId: String, photoId: String) =
        local.deletePropertyPhoto(userId, photoId)

    override suspend fun updatePropertyPhotoUri(userId: String, photoId: String, uri: String) =
        local.updatePropertyPhotoUri(userId, photoId, uri)

    override suspend fun reorderPropertyPhotos(userId: String, propertyId: String, orderedIds: List<String>) =
        local.reorderPropertyPhotos(userId, propertyId, orderedIds)

    override fun attachments(userId: String, propertyId: String): Flow<List<Attachment>> =
        local.attachments(userId, propertyId)

    override suspend fun listAttachments(userId: String, propertyId: String): List<Attachment> =
        local.listAttachments(userId, propertyId)

    override suspend fun addAttachment(userId: String, propertyId: String, name: String?, mime: String?, uri: String) =
        local.addAttachment(userId, propertyId, name, mime, uri)

    override suspend fun deleteAttachment(userId: String, id: String) =
        local.deleteAttachment(userId, id)

    override fun providerWidgets(userId: String, propertyId: String): Flow<List<ProviderWidget>> = flow {
        runCatching { syncPropertyAggregate(userId, propertyId) }
        emitAll(
            utilityProviderDao.observeForProperty(userId, propertyId, active = true)
                .map { providers -> providers.map { it.toProviderWidget() } }
        )
    }

    override fun widgetFields(userId: String, propertyId: String): Flow<List<WidgetField>> = flow {
        runCatching { syncPropertyAggregate(userId, propertyId) }
        emitAll(
            combine(
                utilityProviderDao.observeForProperty(userId, propertyId, active = true),
                customProviderFieldDao.observeForProperty(userId, propertyId)
            ) { providers, customFields ->
                val customByProvider = customFields.groupBy { it.providerId }
                providers.flatMap { provider ->
                    val schemaFields = provider.toWidgetFields()
                    if (schemaFields.isNotEmpty()) {
                        schemaFields
                    } else {
                        customByProvider[provider.id].orEmpty().map { it.toWidgetField() }
                    }
                }
            }
        )
    }

    override fun fieldEntries(userId: String, propertyId: String): Flow<List<FieldEntry>> = flow {
        runCatching { syncPropertyAggregate(userId, propertyId) }
        emitAll(
            combine(
                meterReadingDao.observeForProperty(userId, propertyId),
                utilityProviderDao.observeForProperty(userId, propertyId, active = true)
            ) { readings, providers ->
                val providerIds = providers.map { it.id }.toSet()
                readings
                    .filter { it.providerId in providerIds }
                    .flatMap { it.toFieldEntries() }
            }
        )
    }

    override suspend fun addProviderWidget(userId: String, widget: ProviderWidget, fields: List<WidgetField>) =
        withContext(Dispatchers.IO) {
            val now = OffsetDateTime.now().toString()
            val provider = widget.toUtilityProviderEntity(
                userId = userId,
                fields = fields,
                syncStatus = SYNC_PENDING_CREATE,
                timestamp = now
            )
            utilityProviderDao.upsert(provider)

            if (widget.type == ProviderWidgetType.CUSTOM && fields.isNotEmpty()) {
                customProviderFieldDao.upsertAll(
                    fields.map { field ->
                        field.toCustomProviderFieldEntity(
                            userId = userId,
                            providerId = widget.id,
                            syncStatus = SYNC_PENDING_CREATE,
                            timestamp = now
                        )
                    }
                )
            }
            syncPendingAggregate(userId)
        }

    override suspend fun upsertFieldEntries(userId: String, entries: List<FieldEntry>) =
        withContext(Dispatchers.IO) {
            entries
                .filter { it.fieldId.contains(FIELD_ID_SEPARATOR) }
                .groupBy { it.fieldId.substringBefore(FIELD_ID_SEPARATOR) to (it.periodYear to it.periodMonth) }
                .forEach { (key, groupedEntries) ->
                    val providerId = key.first
                    val (year, month) = key.second
                    val provider = utilityProviderDao.getById(userId, providerId) ?: return@forEach
                    val values = JsonObject()
                    groupedEntries.forEach { entry ->
                        val fieldKey = entry.fieldId.substringAfter(FIELD_ID_SEPARATOR)
                        when {
                            entry.valueNumber != null -> values.add(fieldKey, JsonPrimitive(entry.valueNumber.toString()))
                            !entry.valueText.isNullOrBlank() -> values.add(fieldKey, JsonPrimitive(entry.valueText))
                            !entry.status.isNullOrBlank() -> values.add(fieldKey, JsonPrimitive(entry.status))
                        }
                    }
                    if (values.size() == 0) return@forEach

                    val existing = meterReadingDao.getForProviderPeriod(userId, providerId, year, month)
                    val readingDate = readingDateFor(year, month)
                    val now = OffsetDateTime.now().toString()
                    val nextStatus = if (existing?.syncStatus == SYNC_PENDING_CREATE) {
                        SYNC_PENDING_CREATE
                    } else if (existing == null) {
                        SYNC_PENDING_CREATE
                    } else {
                        SYNC_PENDING_UPDATE
                    }
                    val entity = MeterReadingEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        userId = userId,
                        propertyId = provider.propertyId,
                        providerId = provider.id,
                        readingDate = readingDate.toString(),
                        periodYear = year,
                        periodMonth = month,
                        valuesJson = gson.toJson(values),
                        consumptionJson = existing?.consumptionJson ?: "{}",
                        comment = null,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                        syncStatus = nextStatus,
                        lastSyncError = null,
                        lastSyncAttemptAt = null
                    )
                    meterReadingDao.upsert(entity)
                }
            syncPendingAggregate(userId)
        }

    override suspend fun updateProviderWidgetTitle(userId: String, widgetId: String, title: String) =
        withContext(Dispatchers.IO) {
            val provider = utilityProviderDao.getById(userId, widgetId) ?: return@withContext
            val nextStatus = if (provider.syncStatus == SYNC_PENDING_CREATE) {
                SYNC_PENDING_CREATE
            } else {
                SYNC_PENDING_UPDATE
            }
            utilityProviderDao.upsert(
                provider.copy(
                    title = title,
                    syncStatus = nextStatus,
                    updatedAt = OffsetDateTime.now().toString()
                )
            )
            syncPendingAggregate(userId)
        }

    override suspend fun setProviderWidgetArchived(userId: String, widgetId: String, archived: Boolean) =
        withContext(Dispatchers.IO) {
            val provider = utilityProviderDao.getById(userId, widgetId) ?: return@withContext
            utilityProviderDao.upsert(
                provider.copy(
                    active = !archived,
                    syncStatus = if (archived) SYNC_PENDING_DELETE else SYNC_PENDING_UPDATE,
                    updatedAt = OffsetDateTime.now().toString()
                )
            )
            syncPendingAggregate(userId)
        }

    override suspend fun updateWidgetFields(userId: String, widgetId: String, fields: List<WidgetField>) =
        withContext(Dispatchers.IO) {
            val provider = utilityProviderDao.getById(userId, widgetId) ?: return@withContext
            if (provider.providerType != PROVIDER_TYPE_CUSTOM) return@withContext

            val now = OffsetDateTime.now().toString()
            val existing = customProviderFieldDao.listForProvider(userId, widgetId)
            val incomingIds = fields.map { it.id }.toSet()
            existing
                .filter { it.id !in incomingIds }
                .forEach { field ->
                    if (field.syncStatus == SYNC_PENDING_CREATE) {
                        customProviderFieldDao.delete(userId, field.id)
                    } else {
                        customProviderFieldDao.upsert(
                            field.copy(
                                syncStatus = SYNC_PENDING_DELETE,
                                updatedAt = now
                            )
                        )
                    }
                }

            fields.forEach { field ->
                val existingField = customProviderFieldDao.getById(userId, field.id)
                val nextStatus = if (existingField?.syncStatus == SYNC_PENDING_CREATE || existingField == null) {
                    SYNC_PENDING_CREATE
                } else {
                    SYNC_PENDING_UPDATE
                }
                customProviderFieldDao.upsert(
                    field.toCustomProviderFieldEntity(
                        userId = userId,
                        providerId = widgetId,
                        syncStatus = nextStatus,
                        timestamp = now,
                        existing = existingField
                    )
                )
            }
            val nextProviderStatus = if (provider.syncStatus == SYNC_PENDING_CREATE) {
                SYNC_PENDING_CREATE
            } else {
                SYNC_PENDING_UPDATE
            }
            utilityProviderDao.upsert(
                provider.copy(
                    schemaJson = gson.toJson(customSchema(fields)),
                    syncStatus = nextProviderStatus,
                    updatedAt = now
                )
            )
            syncPendingAggregate(userId)
        }

    private suspend fun syncPendingAggregate(userId: String) {
        aggregateSyncMutex.withLock {
            syncPendingUtilityProvidersLocked(userId)
            syncPendingCustomProviderFieldsLocked(userId)
            syncPendingMeterReadingsLocked(userId)
        }
    }

    private suspend fun syncPendingMeterReadingsLocked(userId: String) {
        meterReadingDao.pending(userId).forEach { pending ->
            try {
                val now = System.currentTimeMillis()
                meterReadingDao.upsert(pending.copy(lastSyncAttemptAt = now, lastSyncError = null))
                when (pending.syncStatus) {
                    SYNC_PENDING_CREATE -> {
                        val created = remoteReadings.create(pending.toRequestDto(gson).copy(id = null))
                        val remoteEntity = created.toEntity(userId, gson)
                        if (remoteEntity == null) {
                            meterReadingDao.upsert(
                                pending.copy(lastSyncError = "Сервер вернул пустой id", lastSyncAttemptAt = now)
                            )
                        } else {
                            if (remoteEntity.id != pending.id) {
                                meterReadingDao.delete(userId, pending.id)
                            }
                            meterReadingDao.upsert(remoteEntity)
                        }
                    }
                    SYNC_PENDING_UPDATE -> {
                        val updated = remoteReadings.patch(pending.id, pending.toRequestDto(gson))
                        updated.toEntity(userId, gson)?.let { meterReadingDao.upsert(it) }
                    }
                    SYNC_PENDING_DELETE -> {
                        remoteReadings.delete(pending.id)
                        meterReadingDao.delete(userId, pending.id)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (pending.syncStatus == SYNC_PENDING_DELETE && error.isRemoteNotFound()) {
                    meterReadingDao.delete(userId, pending.id)
                    Log.d(TAG, "MeterReading tombstone cleaned: readingId=${pending.id}")
                    return@forEach
                }
                val syncError = error.toSyncMessage()
                if (
                    pending.syncStatus == SYNC_PENDING_CREATE &&
                    syncError.isDuplicateSyncConflict() &&
                    reconcilePendingMeterReading(userId, pending)
                ) {
                    return@forEach
                }
                val current = meterReadingDao.getById(userId, pending.id) ?: return@forEach
                if (current.syncStatus == SYNCED) return@forEach
                meterReadingDao.upsert(
                    current.copy(
                        lastSyncError = syncError,
                        lastSyncAttemptAt = System.currentTimeMillis()
                    )
                )
                Log.w(TAG, "meter reading sync failed id=${pending.id} status=${pending.syncStatus}", error)
            }
        }
    }

    private suspend fun syncPendingUtilityProvidersLocked(userId: String) {
        utilityProviderDao.pending(userId).forEach { pending ->
            try {
                val now = System.currentTimeMillis()
                utilityProviderDao.upsert(pending.copy(lastSyncAttemptAt = now))
                when (pending.syncStatus) {
                    SYNC_PENDING_CREATE -> {
                        val created = remoteProviders.create(pending.toUtilityProviderDto())
                        val remoteEntity = created.toEntity(userId, gson)
                        if (remoteEntity == null) {
                            utilityProviderDao.upsert(
                                pending.copy(lastSyncError = "Сервер вернул пустой id", lastSyncAttemptAt = now)
                            )
                        } else {
                            if (remoteEntity.id != pending.id) {
                                customProviderFieldDao.updateProviderId(userId, pending.id, remoteEntity.id)
                                meterReadingDao.updateProviderId(userId, pending.id, remoteEntity.id)
                                utilityProviderDao.delete(userId, pending.id)
                            }
                            utilityProviderDao.upsert(remoteEntity)
                        }
                    }
                    SYNC_PENDING_UPDATE -> {
                        val updated = remoteProviders.patch(pending.id, pending.toUtilityProviderDto())
                        updated.toEntity(userId, gson)?.let { utilityProviderDao.upsert(it) }
                    }
                    SYNC_PENDING_DELETE -> {
                        remoteProviders.delete(pending.id)
                        customProviderFieldDao.deleteAllForProvider(userId, pending.id)
                        utilityProviderDao.delete(userId, pending.id)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (pending.syncStatus == SYNC_PENDING_DELETE && error.isRemoteNotFound()) {
                    customProviderFieldDao.deleteAllForProvider(userId, pending.id)
                    utilityProviderDao.delete(userId, pending.id)
                    Log.d(TAG, "UtilityProvider tombstone cleaned: providerId=${pending.id}")
                    return@forEach
                }
                val syncError = error.toSyncMessage()
                val current = utilityProviderDao.getById(userId, pending.id) ?: return@forEach
                if (current.syncStatus == SYNCED) return@forEach
                utilityProviderDao.upsert(
                    current.copy(
                        lastSyncError = syncError,
                        lastSyncAttemptAt = System.currentTimeMillis()
                    )
                )
                Log.w(TAG, "utility provider sync failed id=${pending.id} status=${pending.syncStatus}", error)
            }
        }
    }

    private suspend fun syncPendingCustomProviderFieldsLocked(userId: String) {
        customProviderFieldDao.pending(userId).forEach { pending ->
            try {
                val provider = utilityProviderDao.getById(userId, pending.providerId)
                if (provider?.syncStatus == SYNC_PENDING_CREATE) return@forEach

                val now = System.currentTimeMillis()
                customProviderFieldDao.upsert(pending.copy(lastSyncAttemptAt = now))
                when (pending.syncStatus) {
                    SYNC_PENDING_CREATE -> {
                        val created = remoteCustomFields.create(pending.toDto(gson).copy(id = null))
                        val remoteEntity = created.toEntity(userId, gson)
                        if (remoteEntity == null) {
                            customProviderFieldDao.upsert(
                                pending.copy(lastSyncError = "Сервер вернул пустой id", lastSyncAttemptAt = now)
                            )
                        } else {
                            if (remoteEntity.id != pending.id) {
                                customProviderFieldDao.delete(userId, pending.id)
                            }
                            customProviderFieldDao.upsert(remoteEntity)
                        }
                    }
                    SYNC_PENDING_UPDATE -> {
                        val updated = remoteCustomFields.patch(pending.id, pending.toDto(gson))
                        updated.toEntity(userId, gson)?.let { customProviderFieldDao.upsert(it) }
                    }
                    SYNC_PENDING_DELETE -> {
                        remoteCustomFields.delete(pending.id)
                        customProviderFieldDao.delete(userId, pending.id)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (pending.syncStatus == SYNC_PENDING_DELETE && error.isRemoteNotFound()) {
                    customProviderFieldDao.delete(userId, pending.id)
                    Log.d(TAG, "CustomProviderField tombstone cleaned: fieldId=${pending.id}")
                    return@forEach
                }
                val syncError = error.toSyncMessage()
                if (
                    pending.syncStatus == SYNC_PENDING_CREATE &&
                    syncError.isDuplicateSyncConflict() &&
                    reconcilePendingCustomProviderField(userId, pending)
                ) {
                    return@forEach
                }
                val current = customProviderFieldDao.getById(userId, pending.id) ?: return@forEach
                if (current.syncStatus == SYNCED) return@forEach
                customProviderFieldDao.upsert(
                    current.copy(
                        lastSyncError = syncError,
                        lastSyncAttemptAt = System.currentTimeMillis()
                    )
                )
                Log.w(TAG, "custom provider field sync failed id=${pending.id} status=${pending.syncStatus}", error)
            }
        }
    }

    private suspend fun reconcilePendingCustomProviderField(
        userId: String,
        pending: CustomProviderFieldEntity
    ): Boolean {
        val remoteEntity = runCatching {
            remoteCustomFields.fetchAll(pending.providerId)
                .firstOrNull { it.key == pending.key }
                ?.toEntity(userId, gson)
        }.getOrNull() ?: return false
        if (remoteEntity.id != pending.id) {
            customProviderFieldDao.delete(userId, pending.id)
        }
        customProviderFieldDao.upsert(remoteEntity)
        Log.d(TAG, "custom field reconciled id=${pending.id} remoteId=${remoteEntity.id} providerId=${pending.providerId} key=${pending.key}")
        return true
    }

    private suspend fun reconcilePendingMeterReading(
        userId: String,
        pending: MeterReadingEntity
    ): Boolean {
        val remoteEntity = runCatching {
            remoteReadings.fetchAll(
                propertyId = pending.propertyId,
                providerId = pending.providerId,
                periodYear = pending.periodYear,
                periodMonth = pending.periodMonth
            )
                .firstOrNull {
                    it.providerId == pending.providerId &&
                        it.periodYear == pending.periodYear &&
                        it.periodMonth == pending.periodMonth
                }
                ?.toEntity(userId, gson)
        }.getOrNull() ?: return false
        if (remoteEntity.id != pending.id) {
            meterReadingDao.delete(userId, pending.id)
        }
        meterReadingDao.upsert(remoteEntity)
        Log.d(TAG, "meter reading reconciled id=${pending.id} remoteId=${remoteEntity.id} providerId=${pending.providerId} period=${pending.periodYear}-${pending.periodMonth}")
        return true
    }

    private suspend fun syncPendingPropertyDetails(userId: String) {
        propertyDetailsDao.pending(userId).forEach { pending ->
            try {
                val now = System.currentTimeMillis()
                propertyDetailsDao.upsert(pending.copy(lastSyncAttemptAt = now, lastSyncError = null))
                val property = propertyDao.getById(userId, pending.propertyId)
                    ?: error("Локальный объект не найден")
                when (pending.syncStatus) {
                    SYNC_PENDING_CREATE,
                    SYNC_PENDING_UPDATE -> {
                        val updated = remoteProperties.update(
                            pending.propertyId,
                            pending.toRequestDto(propertyName = property.name)
                        )
                        val synced = updated.toDetailsEntity(userId)
                            ?: pending.copy(syncStatus = SYNCED, lastSyncError = null, lastSyncAttemptAt = now)
                        propertyDetailsDao.upsert(synced)
                    }
                    SYNC_PENDING_DELETE -> {
                        remoteProperties.update(
                            pending.propertyId,
                            pending.toRequestDto(
                                propertyName = property.name,
                                description = null,
                                areaSqm = null
                            )
                        )
                        propertyDetailsDao.deleteForProperty(userId, pending.propertyId)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (pending.syncStatus == SYNC_PENDING_DELETE && error.isRemoteNotFound()) {
                    propertyDetailsDao.deleteForProperty(userId, pending.propertyId)
                    Log.d(TAG, "PropertyDetails tombstone cleaned: propertyId=${pending.propertyId}")
                    return@forEach
                }
                propertyDetailsDao.upsert(
                    pending.copy(
                        lastSyncError = error.toSyncMessage(),
                        lastSyncAttemptAt = System.currentTimeMillis()
                    )
                )
                Log.w(TAG, "property details sync failed propertyId=${pending.propertyId} status=${pending.syncStatus}", error)
            }
        }
    }
}

private fun TransactionEntity.toRequestDto() =
    transactionRequestDto(
        propertyId = propertyId,
        type = if (isIncome) TxType.INCOME else TxType.EXPENSE,
        amount = amount,
        dateIso = dateIso,
        note = note,
        attachmentUri = attachmentUri,
        attachmentName = attachmentName,
        attachmentMime = attachmentMime
    )

private fun MeterReadingEntity.toRequestDto(gson: Gson): MeterReadingDto =
    MeterReadingDto(
        id = id,
        userId = null,
        propertyId = propertyId,
        providerId = providerId,
        readingDate = readingDate,
        periodYear = periodYear,
        periodMonth = periodMonth,
        values = gson.fromJson(valuesJson, JsonElement::class.java),
        consumption = gson.fromJson(consumptionJson, JsonElement::class.java),
        comment = comment,
        createdAt = null,
        updatedAt = null
    )

private fun PropertyDetailsEntity.toRequestDto(
    propertyName: String,
    description: String? = this.description,
    areaSqm: String? = this.areaSqm
): PropertyRequestDto =
    PropertyRequestDto(
        name = propertyName,
        address = null,
        squareMeters = areaSqm?.trim()?.replace(',', '.')?.toDoubleOrNull(),
        monthlyRent = null,
        coverUri = null,
        leaseFrom = null,
        leaseTo = null,
        description = description,
        areaSqm = areaSqm
    )

private fun Throwable.toSyncMessage(): String =
    when (this) {
        is java.net.ConnectException -> "Сервер не запущен"
        is java.net.SocketTimeoutException -> "Сервер недоступен"
        is java.net.UnknownHostException -> "Неверный адрес сервера"
        is retrofit2.HttpException -> {
            val body = response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }
            body ?: when (code()) {
                401 -> "Требуется повторный вход"
                403 -> "Доступ запрещен"
                in 500..599 -> "Ошибка сервера"
                else -> "Ошибка запроса: ${code()}"
            }
        }
        is java.io.IOException -> "Сервер недоступен"
        else -> message ?: "Ошибка синхронизации"
    }

private fun Throwable.isRemoteNotFound(): Boolean {
    if (this is retrofit2.HttpException && code() == 404) return true
    val normalized = (message ?: "").lowercase()
    return "not found" in normalized ||
        "no " in normalized && " matches the given query" in normalized ||
        "не найден" in normalized
}

private fun String.isDuplicateSyncConflict(): Boolean {
    val normalized = lowercase()
    return "unique" in normalized ||
        "already exists" in normalized ||
        "duplicate" in normalized ||
        "уникаль" in normalized
}

private const val SYNCED = "SYNCED"
private const val SYNC_PENDING_CREATE = "PENDING_CREATE"
private const val SYNC_PENDING_UPDATE = "PENDING_UPDATE"
private const val SYNC_PENDING_DELETE = "PENDING_DELETE"
private const val PROVIDER_TYPE_MOSENERGO = "MOSENERGO"
private const val PROVIDER_TYPE_WATER = "WATER"
private const val PROVIDER_TYPE_CUSTOM = "CUSTOM"
private const val FIELD_ID_SEPARATOR = ":"

private fun UtilityProviderEntity.toProviderWidget(): ProviderWidget =
    ProviderWidget(
        id = id,
        propertyId = propertyId,
        type = if (providerType == PROVIDER_TYPE_CUSTOM) ProviderWidgetType.CUSTOM else ProviderWidgetType.METER_PROVIDER,
        title = title,
        templateKey = templateKey(),
        createdAt = parseRemoteMillis(createdAt) ?: System.currentTimeMillis(),
        archived = !active,
        syncStatus = syncStatus,
        lastSyncError = lastSyncError,
        lastSyncAttemptAt = lastSyncAttemptAt
    )

private fun UtilityProviderEntity.templateKey(): String = when (providerType) {
    PROVIDER_TYPE_WATER -> "uk_water"
    PROVIDER_TYPE_MOSENERGO -> when (mosenergoMode) {
        "DAY_NIGHT" -> "mosenergo_daynight"
        "T1_T2_T3" -> "mosenergo_threetariff"
        else -> "mosenergo_single"
    }
    else -> "custom"
}

private fun UtilityProviderEntity.toWidgetFields(): List<WidgetField> =
    schemaFields(schemaJson).mapIndexed { index, field ->
        WidgetField(
            id = fieldId(id, field.key),
            widgetId = id,
            name = displayFieldLabel(field.key, field.label),
            fieldType = field.fieldType.toWidgetFieldType(),
            unit = displayUnit(field.unit),
            sortOrder = field.sortOrder ?: index
        )
    }

private fun CustomProviderFieldEntity.toWidgetField(): WidgetField =
    WidgetField(
        id = fieldId(providerId, key),
        widgetId = providerId,
        name = displayFieldLabel(key, label),
        fieldType = fieldType.toWidgetFieldType(),
        unit = displayUnit(unit),
        sortOrder = sortOrder
    )

private fun MeterReadingEntity.toFieldEntries(): List<FieldEntry> {
    val values = parseObject(valuesJson) ?: return emptyList()
    return values.entrySet().map { (key, value) ->
        val stringValue = value.asScalarString()
        FieldEntry(
            id = "${id}_$key",
            fieldId = fieldId(providerId, key),
            periodYear = periodYear,
            periodMonth = periodMonth,
            valueNumber = stringValue?.replace(',', '.')?.toDoubleOrNull(),
            valueText = stringValue,
            status = stringValue,
            createdAt = parseRemoteMillis(createdAt) ?: 0L,
            syncStatus = syncStatus,
            lastSyncError = lastSyncError
        )
    }
}

private data class MeterReadingKey(
    val providerId: String,
    val periodYear: Int,
    val periodMonth: Int
)

private fun ProviderWidget.toUtilityProviderDto(fields: List<WidgetField>): UtilityProviderDto {
    val (providerType, mode) = when (templateKey) {
        "uk_water" -> PROVIDER_TYPE_WATER to null
        "mosenergo_daynight" -> PROVIDER_TYPE_MOSENERGO to "DAY_NIGHT"
        "mosenergo_threetariff" -> PROVIDER_TYPE_MOSENERGO to "T1_T2_T3"
        "mosenergo_single" -> PROVIDER_TYPE_MOSENERGO to "SINGLE_TARIFF"
        else -> PROVIDER_TYPE_CUSTOM to null
    }
    return UtilityProviderDto(
        id = id,
        propertyId = propertyId,
        title = title,
        providerType = providerType,
        mosenergoMode = mode,
        configuration = JsonObject(),
        active = !archived,
        schema = customSchema(fields).takeIf { providerType == PROVIDER_TYPE_CUSTOM },
        createdAt = null,
        updatedAt = null
    )
}

private fun ProviderWidget.toUtilityProviderEntity(
    userId: String,
    fields: List<WidgetField>,
    syncStatus: String,
    timestamp: String
): UtilityProviderEntity {
    val dto = toUtilityProviderDto(fields)
    return UtilityProviderEntity(
        id = id,
        userId = userId,
        propertyId = propertyId,
        title = title,
        providerType = dto.providerType.orEmpty().ifBlank { PROVIDER_TYPE_CUSTOM },
        mosenergoMode = dto.mosenergoMode,
        configurationJson = "{}",
        active = !archived,
        schemaJson = gsonlessJson(customSchema(fields)),
        createdAt = timestamp,
        updatedAt = timestamp,
        syncStatus = syncStatus,
        lastSyncError = null,
        lastSyncAttemptAt = null
    )
}

private fun UtilityProviderEntity.toUtilityProviderDto(): UtilityProviderDto =
    UtilityProviderDto(
        id = id,
        propertyId = propertyId,
        title = title,
        providerType = providerType,
        mosenergoMode = mosenergoMode,
        configuration = parseObject(configurationJson) ?: JsonObject(),
        active = active,
        schema = null,
        createdAt = null,
        updatedAt = null
    )

private fun WidgetField.toCustomProviderFieldEntity(
    userId: String,
    providerId: String,
    syncStatus: String,
    timestamp: String,
    existing: CustomProviderFieldEntity? = null
): CustomProviderFieldEntity =
    CustomProviderFieldEntity(
        id = id,
        userId = userId,
        providerId = providerId,
        key = keyPart(),
        label = name,
        fieldType = fieldType.toBackendFieldType(),
        unit = unit,
        required = fieldType == WidgetFieldType.METER || fieldType == WidgetFieldType.MONEY,
        sortOrder = sortOrder,
        configurationJson = existing?.configurationJson ?: "{}",
        createdAt = existing?.createdAt ?: timestamp,
        updatedAt = timestamp,
        syncStatus = syncStatus,
        lastSyncError = existing?.lastSyncError,
        lastSyncAttemptAt = existing?.lastSyncAttemptAt
    )

private fun WidgetField.toCustomProviderFieldDto(providerId: String): CustomProviderFieldDto =
    CustomProviderFieldDto(
        id = id.takeUnless { it.contains(FIELD_ID_SEPARATOR) },
        providerId = providerId,
        key = keyPart(),
        label = name,
        fieldType = fieldType.toBackendFieldType(),
        unit = unit,
        required = fieldType == WidgetFieldType.METER || fieldType == WidgetFieldType.MONEY,
        sortOrder = sortOrder,
        configuration = JsonObject(),
        createdAt = null,
        updatedAt = null
    )

private fun CustomProviderFieldEntity.toDto(gson: Gson): CustomProviderFieldDto =
    CustomProviderFieldDto(
        id = id,
        providerId = providerId,
        key = key,
        label = label,
        fieldType = fieldType,
        unit = unit,
        required = required,
        sortOrder = sortOrder,
        configuration = parseObject(configurationJson) ?: JsonObject(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

private fun gsonlessJson(element: JsonElement): String = element.toString()

private fun customSchema(fields: List<WidgetField>): JsonElement {
    val array = com.google.gson.JsonArray()
    fields.sortedBy { it.sortOrder }.forEach { field ->
        array.add(JsonObject().apply {
            addProperty("key", field.keyPart())
            addProperty("label", field.name)
            addProperty("fieldType", field.fieldType.toBackendFieldType())
            field.unit?.let { addProperty("unit", it) }
            addProperty("required", field.fieldType == WidgetFieldType.METER || field.fieldType == WidgetFieldType.MONEY)
            addProperty("sortOrder", field.sortOrder)
        })
    }
    return array
}

private data class SchemaField(
    val key: String,
    val label: String,
    val fieldType: String,
    val unit: String?,
    val sortOrder: Int?
)

private fun schemaFields(schemaJson: String): List<SchemaField> {
    val parsed = runCatching { JsonParser.parseString(schemaJson) }.getOrNull()
    val array = parsed?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
    return array.mapNotNull { item ->
        val obj = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        val key = obj.stringOrNull("key") ?: return@mapNotNull null
        SchemaField(
            key = key,
            label = obj.stringOrNull("label").orEmpty().ifBlank { key },
            fieldType = (obj.stringOrNull("fieldType") ?: obj.stringOrNull("field_type")).orEmpty().ifBlank { "TEXT" },
            unit = obj.stringOrNull("unit"),
            sortOrder = (obj.get("sortOrder") ?: obj.get("sort_order"))?.asIntOrNull()
        )
    }
}

private fun parseObject(json: String): JsonObject? =
    runCatching { JsonParser.parseString(json) }
        .getOrNull()
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject

private fun JsonObject.stringOrNull(key: String): String? =
    get(key)?.takeUnless { it.isJsonNull }?.asString

private fun JsonElement.asScalarString(): String? = when {
    isJsonNull -> null
    isJsonPrimitive -> asJsonPrimitive.asString
    else -> null
}

private fun JsonElement.asIntOrNull(): Int? =
    runCatching { asInt }.getOrNull()

private fun String.toWidgetFieldType(): WidgetFieldType = when (uppercase()) {
    "COUNTER", "METER" -> WidgetFieldType.METER
    "MONEY", "DECIMAL" -> WidgetFieldType.MONEY
    "STATUS" -> WidgetFieldType.STATUS
    "IMAGE" -> WidgetFieldType.IMAGE
    else -> WidgetFieldType.TEXT
}

private fun WidgetFieldType.toBackendFieldType(): String = when (this) {
    WidgetFieldType.METER -> "COUNTER"
    WidgetFieldType.MONEY -> "MONEY"
    WidgetFieldType.TEXT -> "TEXT"
    WidgetFieldType.STATUS -> "STATUS"
    WidgetFieldType.IMAGE -> "IMAGE"
}

private fun fieldId(providerId: String, key: String): String = "$providerId$FIELD_ID_SEPARATOR$key"

private fun WidgetField.keyPart(): String =
    if (id.contains(FIELD_ID_SEPARATOR)) {
        id.substringAfter(FIELD_ID_SEPARATOR)
    } else {
        "field_${sortOrder + 1}"
    }

private fun displayFieldLabel(key: String, label: String): String = when (key) {
    "total" -> "Показание"
    "day" -> "День"
    "night" -> "Ночь"
    "cold_water" -> "ХВС"
    "hot_water" -> "ГВС"
    else -> label
}

private fun displayUnit(unit: String?): String? = when (unit) {
    "kWh" -> "кВт·ч"
    "m3" -> "м³"
    else -> unit
}

private fun readingDateFor(year: Int, month: Int): LocalDate {
    val today = LocalDate.now()
    return if (today.year == year && today.monthValue == month) today else LocalDate.of(year, month, 1)
}

private fun parseRemoteMillis(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }
}
