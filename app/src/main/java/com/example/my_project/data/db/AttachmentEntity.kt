package com.example.my_project.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val propertyId: String,
    val name: String?,
    /** MIME-тип файла, например: image/jpeg */
    val mimeType: String?,
    /** Строковый URI (content://… или file://…) */
    val uri: String
)