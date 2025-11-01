package com.example.my_project.data

import com.example.my_project.data.model.Attachment
import com.example.my_project.data.model.Property
import com.example.my_project.data.model.Transaction
import com.example.my_project.data.model.TxType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RealEstateRepository {

    // Properties
    fun properties(userId: String): Flow<List<Property>>
    suspend fun addProperty(userId: String, property: Property)
    suspend fun getProperty(userId: String, id: String): Property?
    suspend fun updateProperty(
        userId: String,
        id: String,
        name: String,
        address: String?,
        monthlyRent: Double?
    )

    suspend fun deletePropertyWithRelations(userId: String, id: String)
    suspend fun setPropertyCover(userId: String, propertyId: String, coverUri: String?)

    // Transactions
    fun transactions(userId: String): Flow<List<Transaction>>
    suspend fun transactionsFor(userId: String, propertyId: String): List<Transaction>
    suspend fun addTransaction(userId: String, tx: Transaction)
    suspend fun updateTransaction(
        userId: String,
        id: String,
        type: TxType,
        amount: Double,
        date: LocalDate,
        note: String?
    )

    suspend fun deleteTransaction(userId: String, id: String)

    // Attachments
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