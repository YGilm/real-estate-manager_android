package com.example.real_estate_manager.network.mappers

import com.example.real_estate_manager.data.db.NotificationEntity
import com.example.real_estate_manager.data.db.NotificationRelatedEntityType
import com.example.real_estate_manager.data.db.NotificationType
import com.example.real_estate_manager.data.db.CustomProviderFieldEntity
import com.example.real_estate_manager.data.db.MeterReadingEntity
import com.example.real_estate_manager.data.db.PropertyDocumentEntity
import com.example.real_estate_manager.data.db.PropertyEntity
import com.example.real_estate_manager.data.db.PropertyPhotoEntity
import com.example.real_estate_manager.data.db.PropertyDetailsEntity
import com.example.real_estate_manager.data.db.ReminderRuleEntity
import com.example.real_estate_manager.data.db.TransactionEntity
import com.example.real_estate_manager.data.db.UtilityProviderEntity
import com.example.real_estate_manager.data.model.Property
import com.example.real_estate_manager.data.model.ReminderScheduleMode
import com.example.real_estate_manager.network.dto.CustomProviderFieldDto
import com.example.real_estate_manager.network.dto.MeterReadingDto
import com.example.real_estate_manager.data.model.Transaction
import com.example.real_estate_manager.data.model.TxType
import com.example.real_estate_manager.network.dto.NotificationDto
import com.example.real_estate_manager.network.dto.PropertyDocumentDto
import com.example.real_estate_manager.network.dto.PropertyDto
import com.example.real_estate_manager.network.dto.PropertyPhotoDto
import com.example.real_estate_manager.network.dto.PropertyRequestDto
import com.example.real_estate_manager.network.dto.ReminderDto
import com.example.real_estate_manager.network.dto.ReminderRequestDto
import com.example.real_estate_manager.network.dto.TransactionDto
import com.example.real_estate_manager.network.dto.TransactionRequestDto
import com.example.real_estate_manager.network.dto.UtilityProviderDto
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.example.real_estate_manager.network.util.extractId
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

private fun String?.toEpochMillisOrNull(): Long? =
    try {
        this?.let {
            OffsetDateTime.parse(it).toInstant().toEpochMilli()
        }
    } catch (_: Exception) {
        null
    }

private fun Long?.toOffsetIsoOrNull(): String? =
    this?.let {
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).toString()
    }

fun PropertyDto.toEntity(userId: String): PropertyEntity =
    PropertyEntity(
        id = id.orEmpty(),
        userId = userId,
        name = name.orEmpty().ifBlank { "Без названия" },
        address = address,
        squareMeters = squareMeters ?: areaSqm?.toDoubleOrNull(),
        monthlyRent = monthlyRent,
        pricePerM2 = pricePerM2,
        coverUri = coverUri,
        leaseFrom = leaseFrom,
        leaseTo = leaseTo,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun PropertyDto.toDetailsEntity(userId: String): PropertyDetailsEntity? {
    val propertyId = id?.takeIf { it.isNotBlank() } ?: return null
    val area = areaSqm ?: squareMeters?.toString()
    if (description.isNullOrBlank() && area.isNullOrBlank()) return null
    return PropertyDetailsEntity(
        userId = userId,
        propertyId = propertyId,
        description = description,
        areaSqm = area,
        updatedAt = System.currentTimeMillis(),
        syncStatus = "SYNCED",
        lastSyncError = null,
        lastSyncAttemptAt = null
    )
}

fun Property.toRequestDto(): PropertyRequestDto =
    PropertyRequestDto(
        name = name,
        address = address,
        squareMeters = squareMeters,
        monthlyRent = monthlyRent,
        coverUri = coverUri,
        leaseFrom = leaseFrom,
        leaseTo = leaseTo
    )

private fun JsonElement?.toCacheJson(gson: Gson): String =
    if (this == null || isJsonNull) "{}" else gson.toJson(this)

fun PropertyPhotoDto.toEntity(userId: String): PropertyPhotoEntity? {
    val photoId = id?.takeIf { it.isNotBlank() } ?: return null
    val property = propertyId?.takeIf { it.isNotBlank() } ?: return null
    val ref = imageRef.orEmpty()
    return PropertyPhotoEntity(
        id = photoId,
        userId = userId,
        propertyId = property,
        uri = ref,
        imageRef = imageRef,
        photoType = photoType,
        sortOrder = sortOrder ?: 0,
        createdAt = System.currentTimeMillis(),
        remoteCreatedAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PropertyPhotoEntity.toDto(): PropertyPhotoDto =
    PropertyPhotoDto(
        id,
        propertyId,
        imageRef ?: uri,
        photoType,
        sortOrder,
        remoteCreatedAt,
        updatedAt
    )

fun PropertyDocumentDto.toEntity(userId: String): PropertyDocumentEntity? {
    val documentId = id?.takeIf { it.isNotBlank() } ?: return null
    val property = propertyId?.takeIf { it.isNotBlank() } ?: return null
    return PropertyDocumentEntity(
        id = documentId,
        userId = userId,
        propertyId = property,
        title = title.orEmpty().ifBlank { "Document" },
        fileRef = fileRef.orEmpty(),
        mimeType = mimeType.orEmpty(),
        documentType = documentType,
        uploadedAt = uploadedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun PropertyDocumentEntity.toDto(): PropertyDocumentDto =
    PropertyDocumentDto(
        id,
        propertyId,
        title,
        fileRef,
        mimeType,
        documentType,
        uploadedAt,
        createdAt,
        updatedAt
    )

fun UtilityProviderDto.toEntity(userId: String, gson: Gson): UtilityProviderEntity? {
    val providerId = id?.takeIf { it.isNotBlank() } ?: return null
    val property = propertyId?.takeIf { it.isNotBlank() } ?: return null
    return UtilityProviderEntity(
        id = providerId,
        userId = userId,
        propertyId = property,
        title = title.orEmpty().ifBlank { providerType.orEmpty().ifBlank { "Provider" } },
        providerType = providerType.orEmpty().ifBlank { "CUSTOM" },
        mosenergoMode = mosenergoMode,
        configurationJson = configuration.toCacheJson(gson),
        active = active ?: true,
        schemaJson = schema.toCacheJson(gson),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = "SYNCED",
        lastSyncError = null,
        lastSyncAttemptAt = null
    )
}

fun UtilityProviderEntity.toDto(gson: Gson): UtilityProviderDto =
    UtilityProviderDto(
        id = id,
        propertyId = propertyId,
        title = title,
        providerType = providerType,
        mosenergoMode = mosenergoMode,
        configuration = gson.fromJson(configurationJson, JsonElement::class.java),
        active = active,
        schema = gson.fromJson(schemaJson, JsonElement::class.java),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun CustomProviderFieldDto.toEntity(userId: String, gson: Gson): CustomProviderFieldEntity? {
    val fieldId = id?.takeIf { it.isNotBlank() } ?: return null
    val provider = providerId?.takeIf { it.isNotBlank() } ?: return null
    return CustomProviderFieldEntity(
        id = fieldId,
        userId = userId,
        providerId = provider,
        key = key.orEmpty(),
        label = label.orEmpty().ifBlank { key.orEmpty().ifBlank { "Field" } },
        fieldType = fieldType.orEmpty().ifBlank { "TEXT" },
        unit = unit,
        required = required ?: false,
        sortOrder = sortOrder ?: 0,
        configurationJson = configuration.toCacheJson(gson),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = "SYNCED",
        lastSyncError = null,
        lastSyncAttemptAt = null
    )
}

fun CustomProviderFieldEntity.toDto(gson: Gson): CustomProviderFieldDto =
    CustomProviderFieldDto(
        id = id,
        providerId = providerId,
        key = key,
        label = label,
        fieldType = fieldType,
        unit = unit,
        required = required,
        sortOrder = sortOrder,
        configuration = gson.fromJson(configurationJson, JsonElement::class.java),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun MeterReadingDto.toEntity(fallbackUserId: String, gson: Gson): MeterReadingEntity? {
    val readingId = id?.takeIf { it.isNotBlank() } ?: return null
    val property = propertyId?.takeIf { it.isNotBlank() } ?: return null
    val provider = providerId?.takeIf { it.isNotBlank() } ?: return null
    val year = periodYear ?: return null
    val month = periodMonth?.takeIf { it in 1..12 } ?: return null
    return MeterReadingEntity(
        id = readingId,
        userId = userId?.takeIf { it.isNotBlank() } ?: fallbackUserId,
        propertyId = property,
        providerId = provider,
        readingDate = readingDate.orEmpty(),
        periodYear = year,
        periodMonth = month,
        valuesJson = values.toCacheJson(gson),
        consumptionJson = consumption.toCacheJson(gson),
        comment = comment,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun MeterReadingEntity.toDto(gson: Gson): MeterReadingDto =
    MeterReadingDto(
        id = id,
        userId = userId,
        propertyId = propertyId,
        providerId = providerId,
        readingDate = readingDate,
        periodYear = periodYear,
        periodMonth = periodMonth,
        values = gson.fromJson(valuesJson, JsonElement::class.java),
        consumption = gson.fromJson(consumptionJson, JsonElement::class.java),
        comment = comment,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun TransactionDto.toEntity(userId: String): TransactionEntity =
    TransactionEntity(
        id = id.orEmpty(),
        userId = userId,
        propertyId = propertyId ?: property.extractId().orEmpty(),
        isIncome = isIncome ?: (
                type.equals("INCOME", ignoreCase = true) ||
                        type.equals("income", ignoreCase = true) ||
                        type.equals("доход", ignoreCase = true)
                ),
        amount = amount ?: 0.0,
        dateIso = date?.take(10) ?: LocalDate.now().toString(),
        note = note,
        attachmentUri = attachmentUri,
        attachmentName = attachmentName,
        attachmentMime = attachmentMime,
        syncStatus = "SYNCED",
        lastSyncError = null,
        lastSyncAttemptAt = null
    )

fun Transaction.toRequestDto(): TransactionRequestDto =
    TransactionRequestDto(
        propertyId = propertyId,
        isIncome = type == TxType.INCOME,
        amount = amount,
        dateIso = date.toString(),
        note = note,
        attachmentUri = attachmentUri,
        attachmentName = attachmentName,
        attachmentMime = attachmentMime
    )

fun transactionRequestDto(
    propertyId: String,
    type: TxType,
    amount: Double,
    dateIso: String,
    note: String?,
    attachmentUri: String?,
    attachmentName: String?,
    attachmentMime: String?
): TransactionRequestDto =
    TransactionRequestDto(
        propertyId = propertyId,
        isIncome = type == TxType.INCOME,
        amount = amount,
        dateIso = dateIso,
        note = note,
        attachmentUri = attachmentUri,
        attachmentName = attachmentName,
        attachmentMime = attachmentMime
    )

fun ReminderDto.toEntity(userId: String): ReminderRuleEntity =
    ReminderRuleEntity(
        id = id,
        userId = userId,
        propertyId = propertyId,
        title = title,
        message = message,
        type = type,
        scheduleMode = scheduleMode,
        oneTimeAt = oneTimeAt.toEpochMillisOrNull(),
        rangeStartAt = rangeStartAt,
        rangeEndAt = rangeEndAt,
        dayOfMonth = dayOfMonth,
        rangeStartDay = rangeStartDay,
        rangeEndDay = rangeEndDay,
        repeatEveryDays = repeatEveryDays,
        offsetDays = offsetDays,
        hour = hour,
        minute = minute,
        enabled = enabled,
        nextTriggerAt = nextTriggerAt ?: Long.MAX_VALUE,
        lastFiredAt = lastFiredAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun ReminderRuleEntity.toRequestDto(): ReminderRequestDto {
    val now = System.currentTimeMillis()
    return ReminderRequestDto(
        id = id,
        propertyId = propertyId,
        title = title,
        message = message,
        type = type,
        scheduleMode = scheduleMode,
        oneTimeAt = oneTimeAt
            .takeIf { scheduleMode == ReminderScheduleMode.ONE_TIME.name }
            .takeIf { it != null && it != Long.MAX_VALUE && it > now }
            .toOffsetIsoOrNull(),
        rangeStartAt = rangeStartAt,
        rangeEndAt = rangeEndAt,
        dayOfMonth = dayOfMonth,
        rangeStartDay = rangeStartDay,
        rangeEndDay = rangeEndDay,
        repeatEveryDays = repeatEveryDays,
        offsetDays = offsetDays,
        hour = hour,
        minute = minute,
        enabled = enabled,
        nextTriggerAt = nextTriggerAt.takeIf { it != Long.MAX_VALUE && it > now }
    )
}

fun NotificationDto.toEntity(userId: String): NotificationEntity =
    NotificationEntity(
        id = id,
        userId = userId,
        title = title,
        message = message,
        type = when {
            !ruleId.isNullOrBlank() -> NotificationType.REMINDER.name
            !propertyId.isNullOrBlank() -> NotificationType.PROPERTY.name
            else -> NotificationType.SYSTEM.name
        },
        relatedEntityId = ruleId ?: propertyId,
        relatedEntityType = when {
            !ruleId.isNullOrBlank() -> NotificationRelatedEntityType.REMINDER.name
            !propertyId.isNullOrBlank() -> NotificationRelatedEntityType.PROPERTY.name
            else -> null
        },
        isRead = !isActive,
        createdAt = createdAt,
        readAt = deactivatedAt,
        actionPayload = null,
        updatedAt = createdAt
    )
