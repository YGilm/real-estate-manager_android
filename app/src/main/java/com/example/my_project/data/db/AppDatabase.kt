package com.example.my_project.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Главное правило:
 *  - при каждом изменении схемы (Entity / поля) увеличивай version.
 *  - для production-данных добавляй Migration.
 */
@Database(
    entities = [
        UserEntity::class,
        PropertyEntity::class,
        TransactionEntity::class,
        AttachmentEntity::class
    ],
    version = 3,          // ⬅️ было 1, стало 2
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun propertyDao(): PropertyDao
    abstract fun transactionDao(): TransactionDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        /**
         * Миграция 1 → 2.
         *
         * Предполагается, что в версии 2 в PropertyEntity появились поля:
         *  - leaseFrom: String?
         *  - leaseTo: String?
         *
         * Если ты ещё НЕ добавлял эти поля в Entity, сначала добавь их там.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // новые nullable-поля, поэтому DEFAULT не нужен
                db.execSQL("ALTER TABLE properties ADD COLUMN leaseFrom TEXT")
                db.execSQL("ALTER TABLE properties ADD COLUMN leaseTo TEXT")
            }
        }
    }
}