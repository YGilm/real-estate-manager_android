package com.example.real_estate_manager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.real_estate_manager.auth.UserSession
import com.example.real_estate_manager.data.RealEstateRepository
import com.example.real_estate_manager.data.StatisticsRepository
import com.example.real_estate_manager.data.model.Attachment
import com.example.real_estate_manager.data.model.Property
import com.example.real_estate_manager.data.model.PropertyDetails
import com.example.real_estate_manager.data.model.PropertyPhoto
import com.example.real_estate_manager.data.model.Transaction
import com.example.real_estate_manager.data.model.TxType
import com.example.real_estate_manager.network.dto.StatisticsDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RealEstateViewModel @Inject constructor(
    private val repo: RealEstateRepository,
    private val statisticsRepository: StatisticsRepository,
    session: UserSession
) : ViewModel() {

    private val userIdFlow: StateFlow<String?> =
        session.userIdFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
    private val refreshTrigger = MutableStateFlow(0)

    val properties: StateFlow<List<Property>> =
        userIdFlow
            .combine(refreshTrigger) { uid, refresh -> uid to refresh }
            .flatMapLatest { uid ->
                val userId = uid.first
                if (userId == null) flowOf(emptyList()) else repo.properties(userId)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val transactions: StateFlow<List<Transaction>> =
        userIdFlow
            .combine(refreshTrigger) { uid, refresh -> uid to refresh }
            .flatMapLatest { uid ->
                val userId = uid.first
                if (userId == null) flowOf(emptyList()) else repo.transactions(userId)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private val _backendStatistics = MutableStateFlow<StatisticsDto?>(null)
    val backendStatistics: StateFlow<StatisticsDto?> = _backendStatistics
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    fun refreshCloudData() {
        refreshTrigger.value = refreshTrigger.value + 1
    }

    fun loadStatistics(propertyId: String?, year: Int?, month: Int? = null) {
        launchSafely {
            statisticsRepository.statistics(propertyId, year, month)
                .onSuccess { _backendStatistics.value = it }
                .onFailure { _uiMessage.value = it.toUiErrorMessage() }
        }
    }

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
        launchSafely {
            val id = UUID.randomUUID().toString()
            repo.addProperty(
                uid,
                Property(
                    id = id,
                    name = name,
                    address = address,
                    squareMeters = areaSqm?.trim()?.replace(',', '.')?.toDoubleOrNull(),
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
        launchSafely {
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
        launchSafely {
            repo.deletePropertyWithRelations(uid, id)
        }
    }

    fun setCover(propertyId: String, coverUri: String?) {
        val uid = userIdFlow.value ?: return
        launchSafely {
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
        launchSafely {
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
        launchSafely {
            repo.addPropertyPhotos(uid, propertyId, uris)
        }
    }

    fun deletePropertyPhoto(photoId: String) {
        val uid = userIdFlow.value ?: return
        launchSafely {
            repo.deletePropertyPhoto(uid, photoId)
        }
    }

    fun updatePropertyPhotoUri(photoId: String, uri: String) {
        val uid = userIdFlow.value ?: return
        launchSafely {
            repo.updatePropertyPhotoUri(uid, photoId, uri)
        }
    }

    fun reorderPropertyPhotos(propertyId: String, orderedIds: List<String>) {
        val uid = userIdFlow.value ?: return
        launchSafely {
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
        launchSafely {
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
        launchSafely {
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
        launchSafely {
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
        launchSafely {
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
        launchSafely {
            repo.deleteAttachment(uid, id)
        }
    }

    fun consumeUiMessage() {
        _uiMessage.value = null
    }

    private fun launchSafely(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiMessage.value = error.toUiErrorMessage()
            }
        }
    }
}
