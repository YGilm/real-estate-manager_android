package com.example.real_estate_manager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// ---------- USERS ----------
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Query("UPDATE users SET email = :email WHERE id = :id")
    suspend fun updateEmail(id: String, email: String)

    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = :id")
    suspend fun updatePasswordHash(id: String, passwordHash: String)
}

// ---------- PROPERTIES ----------
@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties WHERE userId = :userId ORDER BY name")
    fun list(userId: String): Flow<List<PropertyEntity>>

    @Query("SELECT * FROM properties WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): PropertyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PropertyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PropertyEntity>)

    @Query("DELETE FROM properties WHERE userId = :userId AND id NOT IN (:ids)")
    suspend fun deleteMissing(userId: String, ids: List<String>)

    @Query("DELETE FROM properties WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Query("DELETE FROM properties WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("UPDATE properties SET coverUri = :coverUri WHERE userId = :userId AND id = :id")
    suspend fun updateCover(userId: String, id: String, coverUri: String?)
}

// ---------- PROPERTY DETAILS ----------
@Dao
interface PropertyDetailsDao {
    @Query("SELECT * FROM property_details WHERE userId = :userId AND propertyId = :propertyId AND syncStatus != 'PENDING_DELETE' LIMIT 1")
    fun observe(userId: String, propertyId: String): Flow<PropertyDetailsEntity?>

    @Query("SELECT * FROM property_details WHERE userId = :userId AND propertyId = :propertyId LIMIT 1")
    suspend fun getById(userId: String, propertyId: String): PropertyDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PropertyDetailsEntity)

    @Query("SELECT * FROM property_details WHERE userId = :userId AND syncStatus != 'SYNCED' ORDER BY lastSyncAttemptAt ASC")
    suspend fun pending(userId: String): List<PropertyDetailsEntity>

    @Query("DELETE FROM property_details WHERE userId = :userId AND propertyId = :propertyId")
    suspend fun deleteForProperty(userId: String, propertyId: String)
}

// ---------- PROPERTY PHOTOS ----------
@Dao
interface PropertyPhotoDao {
    @Query("SELECT * FROM property_photos WHERE userId = :userId AND propertyId = :propertyId ORDER BY createdAt DESC")
    fun observeForProperty(userId: String, propertyId: String): Flow<List<PropertyPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<PropertyPhotoEntity>)

    @Query("UPDATE property_photos SET createdAt = :createdAt WHERE userId = :userId AND id = :id")
    suspend fun updateCreatedAt(userId: String, id: String, createdAt: Long)

    @Query("DELETE FROM property_photos WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("UPDATE property_photos SET uri = :uri WHERE userId = :userId AND id = :id")
    suspend fun updateUri(userId: String, id: String, uri: String)

    @Query("DELETE FROM property_photos WHERE userId = :userId AND propertyId = :propertyId")
    suspend fun deleteForProperty(userId: String, propertyId: String)

    @Query("DELETE FROM property_photos WHERE userId = :userId AND propertyId = :propertyId AND id NOT IN (:ids)")
    suspend fun deleteMissingForProperty(userId: String, propertyId: String, ids: List<String>)
}

@Dao
interface PropertyDocumentDao {
    @Query("SELECT * FROM property_documents WHERE userId = :userId AND propertyId = :propertyId ORDER BY uploadedAt DESC, title")
    fun observeForProperty(userId: String, propertyId: String): Flow<List<PropertyDocumentEntity>>

    @Query("SELECT * FROM property_documents WHERE userId = :userId AND propertyId = :propertyId AND (:documentType IS NULL OR documentType = :documentType) ORDER BY uploadedAt DESC, title")
    fun observeFiltered(userId: String, propertyId: String, documentType: String?): Flow<List<PropertyDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<PropertyDocumentEntity>)

    @Query("DELETE FROM property_documents WHERE userId = :userId AND propertyId = :propertyId")
    suspend fun deleteForProperty(userId: String, propertyId: String)

    @Query("DELETE FROM property_documents WHERE userId = :userId AND propertyId = :propertyId AND id NOT IN (:ids)")
    suspend fun deleteMissingForProperty(userId: String, propertyId: String, ids: List<String>)
}

@Dao
interface UtilityProviderDao {
    @Query("SELECT * FROM utility_providers WHERE userId = :userId AND propertyId = :propertyId AND syncStatus != 'PENDING_DELETE' AND (:active IS NULL OR active = :active) AND (:providerType IS NULL OR providerType = :providerType) ORDER BY title")
    fun observeForProperty(userId: String, propertyId: String, providerType: String? = null, active: Boolean? = null): Flow<List<UtilityProviderEntity>>

    @Query("SELECT * FROM utility_providers WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): UtilityProviderEntity?

    @Query("SELECT * FROM utility_providers WHERE userId = :userId AND propertyId = :propertyId AND syncStatus != 'PENDING_DELETE' ORDER BY title")
    suspend fun listForProperty(userId: String, propertyId: String): List<UtilityProviderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UtilityProviderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<UtilityProviderEntity>)

    @Query("SELECT * FROM utility_providers WHERE userId = :userId AND syncStatus != 'SYNCED' ORDER BY lastSyncAttemptAt ASC")
    suspend fun pending(userId: String): List<UtilityProviderEntity>

    @Query("DELETE FROM utility_providers WHERE userId = :userId AND propertyId = :propertyId AND syncStatus = 'SYNCED'")
    suspend fun deleteForProperty(userId: String, propertyId: String)

    @Query("DELETE FROM utility_providers WHERE userId = :userId AND propertyId = :propertyId AND id NOT IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun deleteMissingForProperty(userId: String, propertyId: String, ids: List<String>)

    @Query("DELETE FROM utility_providers WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)
}

@Dao
interface CustomProviderFieldDao {
    @Query("SELECT * FROM custom_provider_fields WHERE userId = :userId AND providerId = :providerId AND syncStatus != 'PENDING_DELETE' ORDER BY sortOrder, label")
    fun observeForProvider(userId: String, providerId: String): Flow<List<CustomProviderFieldEntity>>

    @Query("SELECT * FROM custom_provider_fields WHERE userId = :userId AND providerId = :providerId AND syncStatus != 'PENDING_DELETE' ORDER BY sortOrder, label")
    suspend fun listForProvider(userId: String, providerId: String): List<CustomProviderFieldEntity>

    @Query(
        "SELECT f.* FROM custom_provider_fields f " +
            "INNER JOIN utility_providers p ON p.userId = f.userId AND p.id = f.providerId " +
            "WHERE f.userId = :userId AND p.propertyId = :propertyId AND p.active = 1 " +
            "AND p.syncStatus != 'PENDING_DELETE' AND f.syncStatus != 'PENDING_DELETE' " +
            "ORDER BY p.title, f.sortOrder, f.label"
    )
    fun observeForProperty(userId: String, propertyId: String): Flow<List<CustomProviderFieldEntity>>

    @Query("SELECT * FROM custom_provider_fields WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): CustomProviderFieldEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomProviderFieldEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<CustomProviderFieldEntity>)

    @Query("SELECT * FROM custom_provider_fields WHERE userId = :userId AND syncStatus != 'SYNCED' ORDER BY lastSyncAttemptAt ASC")
    suspend fun pending(userId: String): List<CustomProviderFieldEntity>

    @Query("DELETE FROM custom_provider_fields WHERE userId = :userId AND providerId = :providerId AND syncStatus = 'SYNCED'")
    suspend fun deleteForProvider(userId: String, providerId: String)

    @Query("DELETE FROM custom_provider_fields WHERE userId = :userId AND providerId = :providerId")
    suspend fun deleteAllForProvider(userId: String, providerId: String)

    @Query("DELETE FROM custom_provider_fields WHERE userId = :userId AND providerId = :providerId AND id NOT IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun deleteMissingForProvider(userId: String, providerId: String, ids: List<String>)

    @Query("DELETE FROM custom_provider_fields WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("DELETE FROM custom_provider_fields WHERE userId = :userId AND id IN (:ids)")
    suspend fun deleteByIds(userId: String, ids: List<String>)

    @Query("UPDATE custom_provider_fields SET providerId = :newProviderId WHERE userId = :userId AND providerId = :oldProviderId")
    suspend fun updateProviderId(userId: String, oldProviderId: String, newProviderId: String)
}

@Dao
interface MeterReadingDao {
    @Query("SELECT * FROM meter_readings WHERE userId = :userId AND propertyId = :propertyId AND syncStatus != 'PENDING_DELETE' ORDER BY periodYear DESC, periodMonth DESC, createdAt DESC")
    fun observeForProperty(userId: String, propertyId: String): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE userId = :userId AND propertyId = :propertyId AND syncStatus != 'PENDING_DELETE' ORDER BY periodYear DESC, periodMonth DESC, createdAt DESC")
    suspend fun listForProperty(userId: String, propertyId: String): List<MeterReadingEntity>

    @Query("SELECT * FROM meter_readings WHERE userId = :userId AND providerId = :providerId AND syncStatus != 'PENDING_DELETE' ORDER BY periodYear DESC, periodMonth DESC, createdAt DESC")
    fun observeForProvider(userId: String, providerId: String): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE userId = :userId AND propertyId = :propertyId AND periodYear = :periodYear AND (:periodMonth IS NULL OR periodMonth = :periodMonth) AND syncStatus != 'PENDING_DELETE' ORDER BY periodMonth DESC")
    fun observeForPeriod(userId: String, propertyId: String, periodYear: Int, periodMonth: Int? = null): Flow<List<MeterReadingEntity>>

    @Query("SELECT * FROM meter_readings WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): MeterReadingEntity?

    @Query("SELECT * FROM meter_readings WHERE userId = :userId AND providerId = :providerId AND periodYear = :periodYear AND periodMonth = :periodMonth LIMIT 1")
    suspend fun getForProviderPeriod(userId: String, providerId: String, periodYear: Int, periodMonth: Int): MeterReadingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MeterReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<MeterReadingEntity>)

    @Query("SELECT * FROM meter_readings WHERE userId = :userId AND syncStatus != 'SYNCED' ORDER BY lastSyncAttemptAt ASC")
    suspend fun pending(userId: String): List<MeterReadingEntity>

    @Query("DELETE FROM meter_readings WHERE userId = :userId AND propertyId = :propertyId AND syncStatus = 'SYNCED'")
    suspend fun deleteForProperty(userId: String, propertyId: String)

    @Query("DELETE FROM meter_readings WHERE userId = :userId AND propertyId = :propertyId AND id NOT IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun deleteMissingForProperty(userId: String, propertyId: String, ids: List<String>)

    @Query("DELETE FROM meter_readings WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("UPDATE meter_readings SET providerId = :newProviderId WHERE userId = :userId AND providerId = :oldProviderId")
    suspend fun updateProviderId(userId: String, oldProviderId: String, newProviderId: String)
}

// ---------- TRANSACTIONS ----------
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId AND syncStatus != 'PENDING_DELETE' ORDER BY dateIso DESC, id DESC")
    fun listAll(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND propertyId = :propertyId AND syncStatus != 'PENDING_DELETE' ORDER BY dateIso DESC, id DESC")
    suspend fun listForProperty(userId: String, propertyId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<TransactionEntity>)

    @Query("SELECT * FROM transactions WHERE userId = :userId AND syncStatus != 'SYNCED' ORDER BY lastSyncAttemptAt ASC")
    suspend fun pending(userId: String): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE userId = :userId AND id NOT IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun deleteMissing(userId: String, ids: List<String>)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Query("DELETE FROM transactions WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("DELETE FROM transactions WHERE userId = :userId AND propertyId = :propertyId")
    suspend fun deleteForProperty(userId: String, propertyId: String)
}

// ---------- ATTACHMENTS (documents) ----------
@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE userId = :userId AND propertyId = :propertyId ORDER BY id DESC")
    fun observeForProperty(userId: String, propertyId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE userId = :userId AND propertyId = :propertyId ORDER BY id DESC")
    suspend fun listForProperty(userId: String, propertyId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("DELETE FROM attachments WHERE userId = :userId AND propertyId = :propertyId")
    suspend fun deleteForProperty(userId: String, propertyId: String)
}

// ---------- PROVIDER WIDGETS ----------
@Dao
interface ProviderWidgetDao {
    @Query(
        "SELECT * FROM provider_widgets " +
            "WHERE userId = :userId AND propertyId = :propertyId AND archived = 0 " +
            "ORDER BY createdAt DESC"
    )
    fun observeForProperty(userId: String, propertyId: String): Flow<List<ProviderWidgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProviderWidgetEntity)

    @Query("UPDATE provider_widgets SET title = :title WHERE userId = :userId AND id = :id")
    suspend fun updateTitle(userId: String, id: String, title: String)

    @Query("UPDATE provider_widgets SET archived = :archived WHERE userId = :userId AND id = :id")
    suspend fun setArchived(userId: String, id: String, archived: Boolean)
}

// ---------- WIDGET FIELDS ----------
@Dao
interface WidgetFieldDao {
    @Query(
        "SELECT * FROM widget_fields " +
            "WHERE userId = :userId AND widgetId = :widgetId " +
            "ORDER BY sortOrder"
    )
    fun observeForWidget(userId: String, widgetId: String): Flow<List<WidgetFieldEntity>>

    @Query(
        "SELECT * FROM widget_fields " +
            "WHERE userId = :userId AND widgetId IN (" +
            "SELECT id FROM provider_widgets WHERE userId = :userId AND propertyId = :propertyId" +
            ") ORDER BY sortOrder"
    )
    fun observeForProperty(userId: String, propertyId: String): Flow<List<WidgetFieldEntity>>

    @Query("SELECT * FROM widget_fields WHERE userId = :userId AND widgetId = :widgetId ORDER BY sortOrder")
    suspend fun listForWidget(userId: String, widgetId: String): List<WidgetFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<WidgetFieldEntity>)

    @Query("DELETE FROM widget_fields WHERE userId = :userId AND id IN (:ids)")
    suspend fun deleteByIds(userId: String, ids: List<String>)
}

// ---------- FIELD ENTRIES ----------
@Dao
interface FieldEntryDao {
    @Query(
        "SELECT * FROM field_entries " +
            "WHERE userId = :userId AND fieldId IN (" +
            "SELECT id FROM widget_fields WHERE userId = :userId AND widgetId IN (" +
            "SELECT id FROM provider_widgets WHERE userId = :userId AND propertyId = :propertyId" +
            ")" +
            ") ORDER BY periodYear DESC, periodMonth DESC"
    )
    fun observeForProperty(userId: String, propertyId: String): Flow<List<FieldEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<FieldEntryEntity>)
}

// ---------- REMINDERS ----------
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId AND syncStatus != 'PENDING_DELETE' ORDER BY nextTriggerAt ASC, createdAt DESC")
    fun observeAll(userId: String): Flow<List<ReminderRuleEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND propertyId = :propertyId AND syncStatus != 'PENDING_DELETE' ORDER BY nextTriggerAt ASC, createdAt DESC")
    fun observeForProperty(userId: String, propertyId: String): Flow<List<ReminderRuleEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND enabled = 1 AND nextTriggerAt <= :nowMillis AND syncStatus != 'PENDING_DELETE' ORDER BY nextTriggerAt ASC")
    suspend fun getDue(userId: String, nowMillis: Long): List<ReminderRuleEntity>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): ReminderRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: ReminderRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<ReminderRuleEntity>)

    @Query("SELECT * FROM reminders WHERE userId = :userId AND syncStatus != 'SYNCED' ORDER BY lastSyncAttemptAt ASC")
    suspend fun pending(userId: String): List<ReminderRuleEntity>

    @Query("SELECT id FROM reminders WHERE userId = :userId")
    suspend fun ids(userId: String): List<String>

    @Query("SELECT id FROM reminders WHERE userId = :userId AND id NOT IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun missingSyncedIds(userId: String, ids: List<String>): List<String>

    @Query("DELETE FROM reminders WHERE userId = :userId AND id NOT IN (:ids) AND syncStatus = 'SYNCED'")
    suspend fun deleteMissing(userId: String, ids: List<String>)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Query("DELETE FROM reminders WHERE userId = :userId AND id = :id")
    suspend fun deleteById(userId: String, id: String)
}

// ---------- NOTIFICATIONS ----------
@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId AND syncStatus != 'DELETED' ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE userId = :userId AND id = :id AND syncStatus != 'DELETED' LIMIT 1")
    fun observeById(userId: String, id: String): Flow<NotificationEntity?>

    @Query("SELECT * FROM notifications WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): NotificationEntity?

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0 AND syncStatus != 'DELETED'")
    fun unreadCount(userId: String): Flow<Int>

    @Query("SELECT * FROM notifications WHERE userId = :userId AND syncStatus != 'DELETED' ORDER BY createdAt DESC")
    fun observeActive(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0 AND syncStatus != 'DELETED'")
    fun countActive(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<NotificationEntity>)

    @Query("DELETE FROM notifications WHERE userId = :userId AND id NOT IN (:ids)")
    suspend fun deleteMissing(userId: String, ids: List<String>)

    @Query("DELETE FROM notifications WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Query("UPDATE notifications SET isRead = 1, readAt = :now, updatedAt = :now WHERE userId = :userId AND id = :id")
    suspend fun markRead(userId: String, id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isRead = 1, readAt = :now, updatedAt = :now WHERE userId = :userId AND syncStatus != 'DELETED'")
    suspend fun markAllRead(userId: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET syncStatus = 'DELETED', updatedAt = :now WHERE userId = :userId AND id = :id")
    suspend fun markDeleted(userId: String, id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isRead = 1, readAt = :now, updatedAt = :now WHERE userId = :userId AND id = :id")
    suspend fun deactivate(userId: String, id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isRead = 1, readAt = :now, updatedAt = :now WHERE userId = :userId AND isRead = 0 AND syncStatus != 'DELETED'")
    suspend fun deactivateAll(userId: String, now: Long = System.currentTimeMillis())
}
