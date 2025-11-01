package com.example.my_project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_project.auth.UserSession
import com.example.my_project.data.RealEstateRepository
import com.example.my_project.data.model.Attachment
import com.example.my_project.data.model.Property
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
    fun addProperty(name: String, address: String?, monthlyRent: Double?) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.addProperty(
                uid,
                Property(name = name, address = address, monthlyRent = monthlyRent)
            )
        }
    }

    suspend fun getProperty(id: String): Property? {
        val uid = userIdFlow.value ?: return null
        return repo.getProperty(uid, id)
    }

    /** Удобная синхронная обёртка, чтобы быстро получить объект из кеша стейта */
    fun getPropertySync(id: String): Property? =
        properties.value.firstOrNull { it.id == id }

    fun updateProperty(id: String, name: String, address: String?, monthlyRent: Double?) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.updateProperty(uid, id, name, address, monthlyRent) }
    }

    fun deleteProperty(id: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.deletePropertyWithRelations(uid, id) }
    }

    fun setCover(propertyId: String, coverUri: String?) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.setPropertyCover(uid, propertyId, coverUri) }
    }

    // ---- Transactions ----
    suspend fun transactionsFor(propertyId: String): List<Transaction> {
        val uid = userIdFlow.value ?: return emptyList()
        return repo.transactionsFor(uid, propertyId)
    }

    /** Сигнатура приведена в соответствие с экраном: с параметром date */
    fun addTransaction(
        propertyId: String,
        isIncome: Boolean,
        amount: Double,
        date: LocalDate,
        note: String?
    ) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch {
            repo.addTransaction(
                uid,
                Transaction(
                    propertyId = propertyId,
                    type = if (isIncome) TxType.INCOME else TxType.EXPENSE,
                    amount = amount,
                    date = date,
                    note = note
                )
            )
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
            repo.updateTransaction(
                uid,
                id,
                if (isIncome) TxType.INCOME else TxType.EXPENSE,
                amount,
                date,
                note
            )
        }
    }

    fun deleteTransaction(id: String) {
        val uid = userIdFlow.value ?: return
        viewModelScope.launch { repo.deleteTransaction(uid, id) }
    }

    // ---- Attachments ----
    suspend fun listAttachments(propertyId: String): List<Attachment> {
        val uid = userIdFlow.value ?: return emptyList()
        return repo.listAttachments(uid, propertyId)
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