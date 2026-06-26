package com.example.real_estate_manager.network.remote

import com.example.real_estate_manager.network.api.TransactionApi
import com.example.real_estate_manager.network.dto.TransactionDto
import com.example.real_estate_manager.network.dto.TransactionRequestDto
import com.example.real_estate_manager.network.util.decodeListEnvelope
import com.google.gson.Gson
import javax.inject.Inject

class RemoteTransactionDataSource @Inject constructor(
    private val api: TransactionApi,
    private val gson: Gson
) {
    suspend fun fetchAll(propertyId: String? = null): List<TransactionDto> =
        gson.decodeListEnvelope(api.list(propertyId))
    suspend fun create(body: TransactionRequestDto): TransactionDto = api.create(body)
    suspend fun update(id: String, body: TransactionRequestDto): TransactionDto = api.update(id, body)
    suspend fun delete(id: String) = api.delete(id)
}
