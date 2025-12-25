package com.example.real_estate_manager.data

import com.example.real_estate_manager.data.model.Attachment
import com.example.real_estate_manager.data.model.Property
import com.example.real_estate_manager.data.model.PropertyDetails
import com.example.real_estate_manager.data.model.PropertyPhoto
import com.example.real_estate_manager.data.model.Transaction
import com.example.real_estate_manager.data.model.TxType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RealEstateRepository {

    // ---------- Properties ----------
    fun properties(userId: String): Flow<List<Property>>
    suspend fun addProperty(userId: String, property: Property)
    suspend fun getProperty(userId: String, id: String): Property?

    suspend fun updateProperty(
        userId: String,
        id: String,
        name: String,
        address: String?,
        monthlyRent: Double?,
        leaseFrom: String?,
        leaseTo: String?
    )

    suspend fun deletePropertyWithRelations(userId: String, id: String)
    suspend fun setPropertyCover(userId: String, propertyId: String, coverUri: String?)

    // ---------- Property details ----------
    fun propertyDetails(userId: String, propertyId: String): Flow<PropertyDetails?>
    suspend fun upsertPropertyDetails(
        userId: String,
        propertyId: String,
        description: String?,
        areaSqm: String?
    )

    // ---------- Property photos ----------
    fun propertyPhotos(userId: String, propertyId: String): Flow<List<PropertyPhoto>>
    suspend fun addPropertyPhotos(userId: String, propertyId: String, uris: List<String>)
    suspend fun deletePropertyPhoto(userId: String, photoId: String)
    suspend fun updatePropertyPhotoUri(userId: String, photoId: String, uri: String)
    suspend fun reorderPropertyPhotos(userId: String, propertyId: String, orderedIds: List<String>)

    // ---------- Transactions ----------
    fun transactions(userId: String): Flow<List<Transaction>>
    suspend fun transactionsFor(userId: String, propertyId: String): List<Transaction>

    suspend fun addTransaction(
        userId: String,
        propertyId: String,
        type: TxType,
        amount: Double,
        date: LocalDate,
        note: String?,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        attachmentMime: String? = null
    )

    suspend fun updateTransaction(
        userId: String,
        id: String,
        type: TxType,
        amount: Double,
        date: LocalDate,
        note: String?,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        attachmentMime: String? = null
    )

    suspend fun deleteTransaction(userId: String, id: String)

    // ---------- Attachments (documents) ----------
    fun attachments(userId: String, propertyId: String): Flow<List<Attachment>>
    suspend fun listAttachments(userId: String, propertyId: String): List<Attachment>

    suspend fun addAttachment(
        userId: String,
        propertyId: String,
        name: String?,
        mime: String?,
        uri: String
    )

    suspend fun deleteAttachment(userId: String, id: String)
}
