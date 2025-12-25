package com.example.real_estate_manager.data.model

data class Attachment(
    val id: String,
    val propertyId: String,
    val name: String?,
    val mimeType: String?,
    val uri: String
)