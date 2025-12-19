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
    version = 5,
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

        // ✅ В версии 3 добавляли attachments (на некоторых базах это могло отсутствовать)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS attachments (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        propertyId TEXT NOT NULL,
                        name TEXT,
                        mimeType TEXT,
                        uri TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_userId_propertyId ON attachments(userId, propertyId)")
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

        // ✅ Вложения для транзакций (счёт/чек) — безопасно, nullable
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN attachmentUri TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN attachmentName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN attachmentMime TEXT")
            }
        }
    }
}