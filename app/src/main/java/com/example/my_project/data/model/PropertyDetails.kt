package com.example.my_project.data.model

data class PropertyDetails(
    val propertyId: String,
    val description: String?,
    /** "45.50" */
    val areaSqm: String?
)