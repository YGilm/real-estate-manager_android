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
        FieldEntryEntity::class,
        ReminderRuleEntity::class,
        NotificationEntity::class,
        PropertyDocumentEntity::class,
        UtilityProviderEntity::class,
        CustomProviderFieldEntity::class,
        MeterReadingEntity::class
    ],
    version = 16,
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
    abstract fun reminderDao(): ReminderDao
    abstract fun notificationDao(): NotificationDao
    abstract fun propertyDocumentDao(): PropertyDocumentDao
    abstract fun utilityProviderDao(): UtilityProviderDao
    abstract fun customProviderFieldDao(): CustomProviderFieldDao
    abstract fun meterReadingDao(): MeterReadingDao

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

        /**
         * 6 -> 7
         * Локальные правила напоминаний.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reminders (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        propertyId TEXT,
                        title TEXT NOT NULL DEFAULT '',
                        message TEXT,
                        type TEXT NOT NULL,
                        scheduleMode TEXT NOT NULL,
                        oneTimeAt INTEGER,
                        rangeStartAt INTEGER,
                        rangeEndAt INTEGER,
                        dayOfMonth INTEGER,
                        rangeStartDay INTEGER,
                        rangeEndDay INTEGER,
                        repeatEveryDays INTEGER,
                        offsetDays INTEGER,
                        hour INTEGER NOT NULL DEFAULT 14,
                        minute INTEGER NOT NULL DEFAULT 0,
                        enabled INTEGER NOT NULL,
                        nextTriggerAt INTEGER NOT NULL,
                        lastFiredAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reminders_userId_nextTriggerAt ON reminders(userId, nextTriggerAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reminders_userId_propertyId ON reminders(userId, propertyId)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN message TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN oneTimeAt INTEGER")
                db.execSQL("ALTER TABLE reminders ADD COLUMN rangeStartAt INTEGER")
                db.execSQL("ALTER TABLE reminders ADD COLUMN rangeEndAt INTEGER")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        propertyId TEXT,
                        ruleId TEXT,
                        title TEXT NOT NULL,
                        message TEXT,
                        createdAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        deactivatedAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_userId_isActive_createdAt ON notifications(userId, isActive, createdAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_userId_propertyId ON notifications(userId, propertyId)"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching { db.execSQL("ALTER TABLE properties ADD COLUMN squareMeters REAL") }
                runCatching { db.execSQL("ALTER TABLE properties ADD COLUMN pricePerM2 REAL") }
                runCatching { db.execSQL("ALTER TABLE properties ADD COLUMN description TEXT") }
                runCatching { db.execSQL("ALTER TABLE properties ADD COLUMN createdAt TEXT") }
                runCatching { db.execSQL("ALTER TABLE properties ADD COLUMN updatedAt TEXT") }

                runCatching { db.execSQL("ALTER TABLE property_photos ADD COLUMN imageRef TEXT") }
                runCatching { db.execSQL("ALTER TABLE property_photos ADD COLUMN photoType TEXT") }
                runCatching { db.execSQL("ALTER TABLE property_photos ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0") }
                runCatching { db.execSQL("ALTER TABLE property_photos ADD COLUMN remoteCreatedAt TEXT") }
                runCatching { db.execSQL("ALTER TABLE property_photos ADD COLUMN updatedAt TEXT") }

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS property_documents (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        propertyId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        fileRef TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        documentType TEXT,
                        uploadedAt TEXT,
                        createdAt TEXT,
                        updatedAt TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_property_documents_userId_propertyId ON property_documents(userId, propertyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_property_documents_userId_propertyId_documentType ON property_documents(userId, propertyId, documentType)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS utility_providers (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        propertyId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        providerType TEXT NOT NULL,
                        mosenergoMode TEXT,
                        configurationJson TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        schemaJson TEXT NOT NULL,
                        createdAt TEXT,
                        updatedAt TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_utility_providers_userId_propertyId ON utility_providers(userId, propertyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_utility_providers_userId_propertyId_active ON utility_providers(userId, propertyId, active)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_utility_providers_userId_propertyId_providerType ON utility_providers(userId, propertyId, providerType)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS custom_provider_fields (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        providerId TEXT NOT NULL,
                        `key` TEXT NOT NULL,
                        label TEXT NOT NULL,
                        fieldType TEXT NOT NULL,
                        unit TEXT,
                        required INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        configurationJson TEXT NOT NULL,
                        createdAt TEXT,
                        updatedAt TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_provider_fields_userId_providerId ON custom_provider_fields(userId, providerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_provider_fields_userId_providerId_sortOrder ON custom_provider_fields(userId, providerId, sortOrder)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS meter_readings (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        propertyId TEXT NOT NULL,
                        providerId TEXT NOT NULL,
                        readingDate TEXT NOT NULL,
                        periodYear INTEGER NOT NULL,
                        periodMonth INTEGER NOT NULL,
                        valuesJson TEXT NOT NULL,
                        consumptionJson TEXT NOT NULL,
                        comment TEXT,
                        createdAt TEXT,
                        updatedAt TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meter_readings_userId_propertyId ON meter_readings(userId, propertyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meter_readings_userId_providerId ON meter_readings(userId, providerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meter_readings_userId_propertyId_periodYear_periodMonth ON meter_readings(userId, propertyId, periodYear, periodMonth)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_meter_readings_userId_providerId_periodYear_periodMonth ON meter_readings(userId, providerId, periodYear, periodMonth)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                }
                runCatching {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN lastSyncError TEXT")
                }
                runCatching {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN lastSyncAttemptAt INTEGER")
                }
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transactions_userId_syncStatus ON transactions(userId, syncStatus)"
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE meter_readings ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                }
                runCatching {
                    db.execSQL("ALTER TABLE meter_readings ADD COLUMN lastSyncError TEXT")
                }
                runCatching {
                    db.execSQL("ALTER TABLE meter_readings ADD COLUMN lastSyncAttemptAt INTEGER")
                }
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_meter_readings_userId_syncStatus ON meter_readings(userId, syncStatus)"
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE reminders ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                }
                runCatching {
                    db.execSQL("ALTER TABLE reminders ADD COLUMN lastSyncError TEXT")
                }
                runCatching {
                    db.execSQL("ALTER TABLE reminders ADD COLUMN lastSyncAttemptAt INTEGER")
                }
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reminders_userId_syncStatus ON reminders(userId, syncStatus)"
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE property_details ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                }
                runCatching {
                    db.execSQL("ALTER TABLE property_details ADD COLUMN lastSyncError TEXT")
                }
                runCatching {
                    db.execSQL("ALTER TABLE property_details ADD COLUMN lastSyncAttemptAt INTEGER")
                }
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_property_details_userId_syncStatus ON property_details(userId, syncStatus)"
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                runCatching {
                    db.execSQL("ALTER TABLE utility_providers ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                }
                runCatching {
                    db.execSQL("ALTER TABLE utility_providers ADD COLUMN lastSyncError TEXT")
                }
                runCatching {
                    db.execSQL("ALTER TABLE utility_providers ADD COLUMN lastSyncAttemptAt INTEGER")
                }
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_utility_providers_userId_syncStatus ON utility_providers(userId, syncStatus)"
                )

                runCatching {
                    db.execSQL("ALTER TABLE custom_provider_fields ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                }
                runCatching {
                    db.execSQL("ALTER TABLE custom_provider_fields ADD COLUMN lastSyncError TEXT")
                }
                runCatching {
                    db.execSQL("ALTER TABLE custom_provider_fields ADD COLUMN lastSyncAttemptAt INTEGER")
                }
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_custom_provider_fields_userId_syncStatus ON custom_provider_fields(userId, syncStatus)"
                )
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notifications_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT,
                        type TEXT NOT NULL,
                        relatedEntityId TEXT,
                        relatedEntityType TEXT,
                        isRead INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        readAt INTEGER,
                        actionPayload TEXT,
                        syncStatus TEXT NOT NULL,
                        lastSyncError TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO notifications_new (
                        id,
                        userId,
                        title,
                        message,
                        type,
                        relatedEntityId,
                        relatedEntityType,
                        isRead,
                        createdAt,
                        readAt,
                        actionPayload,
                        syncStatus,
                        lastSyncError,
                        updatedAt
                    )
                    SELECT
                        id,
                        userId,
                        title,
                        message,
                        CASE
                            WHEN ruleId IS NOT NULL AND ruleId != '' THEN 'REMINDER'
                            WHEN propertyId IS NOT NULL AND propertyId != '' THEN 'PROPERTY'
                            ELSE 'SYSTEM'
                        END,
                        CASE
                            WHEN ruleId IS NOT NULL AND ruleId != '' THEN ruleId
                            WHEN propertyId IS NOT NULL AND propertyId != '' THEN propertyId
                            ELSE NULL
                        END,
                        CASE
                            WHEN ruleId IS NOT NULL AND ruleId != '' THEN 'REMINDER'
                            WHEN propertyId IS NOT NULL AND propertyId != '' THEN 'PROPERTY'
                            ELSE NULL
                        END,
                        CASE WHEN isActive = 0 THEN 1 ELSE 0 END,
                        createdAt,
                        CASE WHEN isActive = 0 THEN deactivatedAt ELSE NULL END,
                        NULL,
                        'SYNCED',
                        NULL,
                        createdAt
                    FROM notifications
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE notifications")
                db.execSQL("ALTER TABLE notifications_new RENAME TO notifications")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_userId_isRead_createdAt ON notifications(userId, isRead, createdAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_userId_relatedEntityType_relatedEntityId ON notifications(userId, relatedEntityType, relatedEntityId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_userId_type_createdAt ON notifications(userId, type, createdAt)"
                )
            }
        }
    }
}
