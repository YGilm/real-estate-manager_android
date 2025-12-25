package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.auth.UserSession
import com.example.real_estate_manager.data.RealEstateRepository
import com.example.real_estate_manager.data.model.Attachment
import com.example.real_estate_manager.data.model.Property
import com.example.real_estate_manager.data.model.PropertyDetails
import com.example.real_estate_manager.data.model.PropertyPhoto
import com.example.real_estate_manager.data.model.Transaction
import com.example.real_estate_manager.data.model.TxType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RealEstateViewModel @Inject constructor(
    private val repo: RealEstateRepository,
    session: UserSession
) : ViewModel() {

    private val userIdFlow: StateFlow<String?> =
        session.userIdFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val properties: StateFlow<List<Property>> =
        userIdFlow
            .flatMapLatest { uid ->
                if (uid == null) flowOf(emptyList()) else repo.properties(uid)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val transactions: StateFlow<List<Transaction>> =
        userIdFlow
            .flatMapLatest { uid ->
                if (uid == null) flowOf(emptyList()) else repo.transactions(uid)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // --------------------------------------------------------------------
    // Property
    // --------------------------------------------------------------------

    fun addProperty(
        name: String,
        address: String?,
        monthlyRent: Double?,
        areaSqm: String?,
        leaseFrom: String? = null,
        leaseTo: String? = null,
        coverUri: String? = null
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            repo.addProperty(
                uid,
                Property(
                    id = id,
                    name = name,
                    address = address,
                    monthlyRent = monthlyRent,
                    leaseFrom = leaseFrom,
                    leaseTo = leaseTo,
                    coverUri = coverUri
                )
            )
            if (!areaSqm.isNullOrBlank()) {
                repo.upsertPropertyDetails(
                    userId = uid,
                    propertyId = id,
                    description = null,
                    areaSqm = areaSqm
                )
            }
        }
    }

    suspend fun getProperty(id: String): Property? {
        val uid = userIdFlow.value ?: return null
        return repo.getProperty(uid, id)
    }

    fun updateProperty(
        id: String,
        name: String,
        address: String?,
        monthlyRent: Double?,
        leaseFrom: String?,
        leaseTo: String?
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.updateProperty(
                userId = uid,
                id = id,
                name = name,
                address = address,
                monthlyRent = monthlyRent,
                leaseFrom = leaseFrom,
                leaseTo = leaseTo
            )
        }
    }

    fun deleteProperty(id: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.deletePropertyWithRelations(uid, id)
        }
    }

    fun setCover(propertyId: String, coverUri: String?) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.setPropertyCover(uid, propertyId, coverUri)
        }
    }

    // --------------------------------------------------------------------
    // Property details
    // --------------------------------------------------------------------

    fun propertyDetails(propertyId: String): Flow<PropertyDetails?> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(null) else repo.propertyDetails(uid, propertyId)
        }

    fun savePropertyDetails(
        propertyId: String,
        description: String?,
        areaSqm: String?
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.upsertPropertyDetails(
                userId = uid,
                propertyId = propertyId,
                description = description,
                areaSqm = areaSqm
            )
        }
    }

    // --------------------------------------------------------------------
    // Property photos
    // --------------------------------------------------------------------

    fun propertyPhotos(propertyId: String): Flow<List<PropertyPhoto>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.propertyPhotos(uid, propertyId)
        }

    fun addPropertyPhotos(propertyId: String, uris: List<String>) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.addPropertyPhotos(uid, propertyId, uris)
        }
    }

    fun deletePropertyPhoto(photoId: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.deletePropertyPhoto(uid, photoId)
        }
    }

    fun updatePropertyPhotoUri(photoId: String, uri: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.updatePropertyPhotoUri(uid, photoId, uri)
        }
    }

    fun reorderPropertyPhotos(propertyId: String, orderedIds: List<String>) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.reorderPropertyPhotos(
                userId = uid,
                propertyId = propertyId,
                orderedIds = orderedIds
            )
        }
    }

    // --------------------------------------------------------------------
    // Transactions
    // --------------------------------------------------------------------

    fun addTransaction(
        propertyId: String,
        isIncome: Boolean,
        amount: Double,
        date: LocalDate,
        note: String?,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        attachmentMime: String? = null
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.addTransaction(
                userId = uid,
                propertyId = propertyId,
                type = if (isIncome) TxType.INCOME else TxType.EXPENSE,
                amount = amount,
                date = date,
                note = note,
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                attachmentMime = attachmentMime
            )
        }
    }

    fun updateTransaction(
        id: String,
        isIncome: Boolean,
        amount: Double,
        date: LocalDate,
        note: String?,
        attachmentUri: String? = null,
        attachmentName: String? = null,
        attachmentMime: String? = null
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.updateTransaction(
                userId = uid,
                id = id,
                type = if (isIncome) TxType.INCOME else TxType.EXPENSE,
                amount = amount,
                date = date,
                note = note,
                attachmentUri = attachmentUri,
                attachmentName = attachmentName,
                attachmentMime = attachmentMime
            )
        }
    }

    fun deleteTransaction(id: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.deleteTransaction(uid, id)
        }
    }

    // --------------------------------------------------------------------
    // Attachments (документы)
    // --------------------------------------------------------------------

    fun attachments(propertyId: String): Flow<List<Attachment>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.attachments(uid, propertyId)
        }

    fun addAttachment(
        propertyId: String,
        name: String?,
        mime: String?,
        uri: String
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.addAttachment(
                userId = uid,
                propertyId = propertyId,
                name = name,
                mime = mime,
                uri = uri
            )
        }
    }

    fun deleteAttachment(id: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.deleteAttachment(uid, id)
        }
    }
}
