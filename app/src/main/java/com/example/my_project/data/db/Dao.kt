package com.example.my_project.data.db

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

    @Query("DELETE FROM property_photos WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

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

    // оставляем старый API (может быть где-то используется)
    @Query("SELECT * FROM attachments WHERE userId = :userId AND propertyId = :propertyId ORDER BY id DESC")
    suspend fun listForProperty(userId: String, propertyId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE userId = :userId AND id = :id")
    suspend fun delete(userId: String, id: String)

    @Query("DELETE FROM attachments WHERE userId = :userId AND propertyId = :propertyId")
    suspend fun deleteForProperty(userId: String, propertyId: String)
}