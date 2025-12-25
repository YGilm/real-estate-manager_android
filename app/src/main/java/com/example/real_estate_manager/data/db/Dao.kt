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

    @Query("DELETE FROM properties WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("UPDATE properties SET coverUri = :coverUri WHERE userId = :userId AND id = :id")
    suspend fun updateCover(userId: String, id: String, coverUri: String?)
}

// ---------- PROPERTY DETAILS ----------
@Dao
interface PropertyDetailsDao {
    @Query("SELECT * FROM property_details WHERE userId = :userId AND propertyId = :propertyId LIMIT 1")
    fun observe(userId: String, propertyId: String): Flow<PropertyDetailsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PropertyDetailsEntity)

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
}

// ---------- TRANSACTIONS ----------
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY dateIso DESC, id DESC")
    fun listAll(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND propertyId = :propertyId ORDER BY dateIso DESC, id DESC")
    suspend fun listForProperty(userId: String, propertyId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND id = :id LIMIT 1")
    suspend fun getById(userId: String, id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

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
