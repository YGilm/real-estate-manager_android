package com.example.real_estate_manager.di

import android.content.Context
import androidx.room.Room
import com.example.real_estate_manager.data.db.AppDatabase
import com.example.real_estate_manager.data.db.AttachmentDao
import com.example.real_estate_manager.data.db.CustomProviderFieldDao
import com.example.real_estate_manager.data.db.MeterReadingDao
import com.example.real_estate_manager.data.db.PropertyDao
import com.example.real_estate_manager.data.db.PropertyDocumentDao
import com.example.real_estate_manager.data.db.PropertyDetailsDao
import com.example.real_estate_manager.data.db.PropertyPhotoDao
import com.example.real_estate_manager.data.db.ProviderWidgetDao
import com.example.real_estate_manager.data.db.ReminderDao
import com.example.real_estate_manager.data.db.TransactionDao
import com.example.real_estate_manager.data.db.UserDao
import com.example.real_estate_manager.data.db.WidgetFieldDao
import com.example.real_estate_manager.data.db.FieldEntryDao
import com.example.real_estate_manager.data.db.NotificationDao
import com.example.real_estate_manager.data.db.UtilityProviderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "real_estate.db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16
            )
            .build()

    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun providePropertyDao(db: AppDatabase): PropertyDao = db.propertyDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()
    @Provides fun providePropertyDetailsDao(db: AppDatabase): PropertyDetailsDao = db.propertyDetailsDao()
    @Provides fun providePropertyPhotoDao(db: AppDatabase): PropertyPhotoDao = db.propertyPhotoDao()
    @Provides fun provideProviderWidgetDao(db: AppDatabase): ProviderWidgetDao = db.providerWidgetDao()
    @Provides fun provideWidgetFieldDao(db: AppDatabase): WidgetFieldDao = db.widgetFieldDao()
    @Provides fun provideFieldEntryDao(db: AppDatabase): FieldEntryDao = db.fieldEntryDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()
    @Provides fun providePropertyDocumentDao(db: AppDatabase): PropertyDocumentDao = db.propertyDocumentDao()
    @Provides fun provideUtilityProviderDao(db: AppDatabase): UtilityProviderDao = db.utilityProviderDao()
    @Provides fun provideCustomProviderFieldDao(db: AppDatabase): CustomProviderFieldDao = db.customProviderFieldDao()
    @Provides fun provideMeterReadingDao(db: AppDatabase): MeterReadingDao = db.meterReadingDao()
}
