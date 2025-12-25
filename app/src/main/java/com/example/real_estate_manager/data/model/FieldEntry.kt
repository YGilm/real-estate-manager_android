package com.example.real_estate_manager.data.model

data class FieldEntry(
    val id: String,
    val fieldId: String,
    val periodYear: Int,
    val periodMonth: Int,
    val valueNumber: Double? = null,
    val valueText: String? = null,
    val status: String? = null,
    val createdAt: Long
)
