package com.example.my_project.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        PropertyEntity::class,
        TransactionEntity::class,
        AttachmentEntity::class,
        PropertyDetailsEntity::class,
        PropertyPhotoEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun propertyDao(): PropertyDao
    abstract fun transactionDao(): TransactionDao
    abstract fun attachmentDao(): AttachmentDao

    abstract fun propertyDetailsDao(): PropertyDetailsDao
    abstract fun propertyPhotoDao(): PropertyPhotoDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE properties ADD COLUMN leaseFrom TEXT")
                db.execSQL("ALTER TABLE properties ADD COLUMN leaseTo TEXT")
            }
        }

        // ✅ Добавляем таблицы для деталей и фото
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS property_details (
                        userId TEXT NOT NULL,
                        propertyId TEXT NOT NULL,
                        description TEXT,
                        areaSqm TEXT,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(userId, propertyId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS property_photos (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        propertyId TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_property_photos_userId_propertyId ON property_photos(userId, propertyId)")
            }
        }
    }
}