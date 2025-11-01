package com.example.my_project.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * УВЕЛИЧЬ version при каждом изменении схемы.
 * Сейчас поставим 2 (было 1).
 */
@Database(
    entities = [
        UserEntity::class,
        PropertyEntity::class,
        TransactionEntity::class,
        AttachmentEntity::class
    ],
    version = 2,                 // ⬅️ повысили версию
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun propertyDao(): PropertyDao
    abstract fun transactionDao(): TransactionDao
    abstract fun attachmentDao(): AttachmentDao
}