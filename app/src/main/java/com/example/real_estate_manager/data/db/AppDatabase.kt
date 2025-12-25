package com.example.real_estate_manager.data.db

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
        PropertyPhotoEntity::class,
        ProviderWidgetEntity::class,
        WidgetFieldEntity::class,
        FieldEntryEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun propertyDao(): PropertyDao
    abstract fun transactionDao(): TransactionDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun propertyDetailsDao(): PropertyDetailsDao
    abstract fun propertyPhotoDao(): PropertyPhotoDao
    abstract fun providerWidgetDao(): ProviderWidgetDao
    abstract fun widgetFieldDao(): WidgetFieldDao
    abstract fun fieldEntryDao(): FieldEntryDao

    companion object {

        /**
         * 1 -> 2
         * Добавили leaseFrom / leaseTo в properties
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching { db.execSQL("ALTER TABLE properties ADD COLUMN leaseFrom TEXT") }
                runCatching { db.execSQL("ALTER TABLE properties ADD COLUMN leaseTo TEXT") }
            }
        }

        /**
         * 2 -> 3
         * Страховочная миграция: гарантирует users + legacy-аккаунт,
         * и гарантирует наличие userId в основных таблицах.
         *
         * Нужна на случай, если у кого-то база была старой версии.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // users
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        passwordHash TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // legacy user (чтобы старые данные не потерялись и были видны)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO users(id, email, passwordHash)
                    VALUES('legacy', 'legacy@local', 'legacy')
                    """.trimIndent()
                )

                // properties.userId
                runCatching {
                    db.execSQL("ALTER TABLE properties ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy'")
                }
                runCatching {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_properties_userId ON properties(userId)")
                }

                // transactions.userId
                runCatching {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN userId TEXT NOT NULL DEFAULT 'legacy'")
                }
                runCatching {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_userId_propertyId ON transactions(userId, propertyId)")
                }

                // attachments.userId (таблица могла существовать уже)
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
                runCatching {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_userId_propertyId ON attachments(userId, propertyId)")
                }
            }
        }

        /**
         * 3 -> 4
         * Добавили таблицы property_details и property_photos + индексы
         */
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

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_property_photos_userId_propertyId ON property_photos(userId, propertyId)"
                )
            }
        }

        /**
         * 4 -> 5
         * Страховочно: если используешь вложения прямо в transactions — добавит колонки.
         * Даже если пока в Entity их нет — лишние колонки Room обычно не ломают.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching { db.execSQL("ALTER TABLE transactions ADD COLUMN attachmentUri TEXT") }
                runCatching { db.execSQL("ALTER TABLE transactions ADD COLUMN attachmentName TEXT") }
                runCatching { db.execSQL("ALTER TABLE transactions ADD COLUMN attachmentMime TEXT") }
            }
        }

        /**
         * 5 -> 6
         * Добавили таблицы для показаний (виджеты, поля, записи).
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS provider_widgets (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        propertyId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        templateKey TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        archived INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_provider_widgets_userId_propertyId ON provider_widgets(userId, propertyId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS widget_fields (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        widgetId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        fieldType TEXT NOT NULL,
                        unit TEXT,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_widget_fields_userId_widgetId ON widget_fields(userId, widgetId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS field_entries (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        fieldId TEXT NOT NULL,
                        periodYear INTEGER NOT NULL,
                        periodMonth INTEGER NOT NULL,
                        valueNumber REAL,
                        valueText TEXT,
                        status TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_field_entries_userId_fieldId ON field_entries(userId, fieldId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_field_entries_userId_fieldId_period ON field_entries(userId, fieldId, periodYear, periodMonth)"
                )
            }
        }
    }
}
