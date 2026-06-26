package com.example.real_estate_manager.data

import android.util.Log
import com.example.real_estate_manager.data.db.CustomProviderFieldDao
import com.example.real_estate_manager.data.db.CustomProviderFieldEntity
import com.example.real_estate_manager.data.db.MeterReadingDao
import com.example.real_estate_manager.data.db.MeterReadingEntity
import com.example.real_estate_manager.data.db.PropertyDocumentDao
import com.example.real_estate_manager.data.db.PropertyDocumentEntity
import com.example.real_estate_manager.data.db.PropertyPhotoDao
import com.example.real_estate_manager.data.db.PropertyPhotoEntity
import com.example.real_estate_manager.data.db.UtilityProviderDao
import com.example.real_estate_manager.data.db.UtilityProviderEntity
import com.example.real_estate_manager.network.mappers.toEntity
import com.example.real_estate_manager.network.remote.RemoteCustomProviderFieldDataSource
import com.example.real_estate_manager.network.remote.RemoteMeterReadingDataSource
import com.example.real_estate_manager.network.remote.RemotePropertyDocumentDataSource
import com.example.real_estate_manager.network.remote.RemotePropertyPhotoDataSource
import com.example.real_estate_manager.network.remote.RemoteUtilityProviderDataSource
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
class AggregateSyncRepository @Inject constructor(
    private val propertyPhotoDao: PropertyPhotoDao,
    private val propertyDocumentDao: PropertyDocumentDao,
    private val utilityProviderDao: UtilityProviderDao,
    private val customProviderFieldDao: CustomProviderFieldDao,
    private val meterReadingDao: MeterReadingDao,
    private val remotePhotos: RemotePropertyPhotoDataSource,
    private val remoteDocuments: RemotePropertyDocumentDataSource,
    private val remoteProviders: RemoteUtilityProviderDataSource,
    private val remoteCustomFields: RemoteCustomProviderFieldDataSource,
    private val remoteReadings: RemoteMeterReadingDataSource,
    private val gson: Gson
) {
    private companion object {
        const val TAG = "AggregateSync"
    }

    fun photos(userId: String, propertyId: String): Flow<List<PropertyPhotoEntity>> =
        propertyPhotoDao.observeForProperty(userId, propertyId)

    fun documents(userId: String, propertyId: String): Flow<List<PropertyDocumentEntity>> =
        propertyDocumentDao.observeForProperty(userId, propertyId)

    fun providers(userId: String, propertyId: String, providerType: String? = null, active: Boolean? = null): Flow<List<UtilityProviderEntity>> =
        utilityProviderDao.observeForProperty(userId, propertyId, providerType, active)

    fun customFields(userId: String, providerId: String): Flow<List<CustomProviderFieldEntity>> =
        customProviderFieldDao.observeForProvider(userId, providerId)

    fun readings(userId: String, propertyId: String): Flow<List<MeterReadingEntity>> =
        meterReadingDao.observeForProperty(userId, propertyId)

    suspend fun syncProperty(userId: String, propertyId: String) = withContext(Dispatchers.IO) {
        val photos = remotePhotos.fetchAll(propertyId).mapNotNull { it.toEntity(userId) }
        propertyPhotoDao.upsertAll(photos)
        if (photos.isEmpty()) propertyPhotoDao.deleteForProperty(userId, propertyId)
        else propertyPhotoDao.deleteMissingForProperty(userId, propertyId, photos.map { it.id })

        val documents = remoteDocuments.fetchAll(propertyId).mapNotNull { it.toEntity(userId) }
        propertyDocumentDao.upsertAll(documents)
        if (documents.isEmpty()) propertyDocumentDao.deleteForProperty(userId, propertyId)
        else propertyDocumentDao.deleteMissingForProperty(userId, propertyId, documents.map { it.id })

        val providers = remoteProviders.fetchAll(propertyId).mapNotNull { it.toEntity(userId, gson) }
        utilityProviderDao.upsertAll(providers)
        if (providers.isEmpty()) utilityProviderDao.deleteForProperty(userId, propertyId)
        else utilityProviderDao.deleteMissingForProperty(userId, propertyId, providers.map { it.id })
        Log.d(TAG, "UtilityProviderEntity propertyId=$propertyId count=${utilityProviderDao.listForProperty(userId, propertyId).size}")

        providers.forEach { provider ->
            val fields = remoteCustomFields.fetchAll(provider.id).mapNotNull { it.toEntity(userId, gson) }
            customProviderFieldDao.upsertAll(fields)
            if (fields.isEmpty()) customProviderFieldDao.deleteForProvider(userId, provider.id)
            else customProviderFieldDao.deleteMissingForProvider(userId, provider.id, fields.map { it.id })
            Log.d(TAG, "CustomProviderFieldEntity providerId=${provider.id} count=${customProviderFieldDao.listForProvider(userId, provider.id).size}")
        }

        val pendingReadingKeys = meterReadingDao.pending(userId)
            .filter { it.propertyId == propertyId }
            .map { ReadingKey(it.providerId, it.periodYear, it.periodMonth) }
            .toSet()
        val readings = remoteReadings.fetchAll(propertyId = propertyId)
            .mapNotNull { it.toEntity(userId, gson) }
            .filter { ReadingKey(it.providerId, it.periodYear, it.periodMonth) !in pendingReadingKeys }
        meterReadingDao.upsertAll(readings)
        if (readings.isEmpty()) meterReadingDao.deleteForProperty(userId, propertyId)
        else meterReadingDao.deleteMissingForProperty(userId, propertyId, readings.map { it.id })
        Log.d(TAG, "MeterReadingEntity propertyId=$propertyId count=${meterReadingDao.listForProperty(userId, propertyId).size}")
        Log.d(TAG, "aggregate cached propertyId=$propertyId photos=${photos.size} documents=${documents.size} providers=${providers.size} readings=${readings.size}")
    }
}

private data class ReadingKey(
    val providerId: String,
    val periodYear: Int,
    val periodMonth: Int
)
