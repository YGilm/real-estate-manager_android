package com.example.my_project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_project.auth.UserSession
import com.example.my_project.data.RealEstateRepository
import com.example.my_project.data.model.Attachment
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.PropertyDetails
import com.example.my_project.data.model.PropertyPhoto
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class RealEstateViewModel @Inject constructor(
    private val repo: RealEstateRepository,
    session: UserSession
) : ViewModel() {

    private val userIdFlow: StateFlow<String?> =
        session.userIdFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val properties: StateFlow<List<Property>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.properties(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<Transaction>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.transactions(uid)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Property ----
    fun addProperty(
        name: String,
        address: String?,
        monthlyRent: Double?,
        leaseFrom: String? = null,
        leaseTo: String? = null,
        coverUri: String? = null
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.addProperty(
                uid,
                Property(
                    name = name,
                    address = address,
                    monthlyRent = monthlyRent,
                    leaseFrom = leaseFrom,
                    leaseTo = leaseTo,
                    coverUri = coverUri
                )
            )
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
            repo.updateProperty(uid, id, name, address, monthlyRent, leaseFrom, leaseTo)
        }
    }

    fun deleteProperty(id: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.deletePropertyWithRelations(uid, id) }
    }

    fun setCover(propertyId: String, coverUri: String?) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.setPropertyCover(uid, propertyId, coverUri) }
    }

    // ---- Property Details ----
    fun propertyDetails(propertyId: String): Flow<PropertyDetails?> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(null) else repo.propertyDetails(uid, propertyId)
        }

    fun savePropertyDetails(propertyId: String, description: String?, areaSqm: String?) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.upsertPropertyDetails(uid, propertyId, description, areaSqm)
        }
    }

    fun propertyPhotos(propertyId: String): Flow<List<PropertyPhoto>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.propertyPhotos(uid, propertyId)
        }

    fun addPropertyPhotos(propertyId: String, uris: List<String>) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.addPropertyPhotos(uid, propertyId, uris) }
    }

    fun deletePropertyPhoto(photoId: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.deletePropertyPhoto(uid, photoId) }
    }

    // ---- Transactions ----
    fun addTransaction(
        propertyId: String,
        isIncome: Boolean,
        amount: Double,
        date: LocalDate,
        note: String?
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.addTransaction(uid, propertyId, if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, date, note)
        }
    }

    fun updateTransaction(
        id: String,
        isIncome: Boolean,
        amount: Double,
        date: LocalDate,
        note: String?
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.updateTransaction(uid, id, if (isIncome) TxType.INCOME else TxType.EXPENSE, amount, date, note)
        }
    }

    fun deleteTransaction(id: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.deleteTransaction(uid, id) }
    }

    // ---- Attachments (documents) ----
    fun attachments(propertyId: String): Flow<List<Attachment>> =
        userIdFlow.flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else repo.attachments(uid, propertyId)
        }

    fun addAttachment(propertyId: String, name: String?, mime: String?, uri: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.addAttachment(uid, propertyId, name, mime, uri) }
    }

    fun deleteAttachment(id: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.deleteAttachment(uid, id) }
    }
}