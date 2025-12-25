package com.example.real_estate_manager.di

import android.content.Context
import androidx.room.Room
import com.example.real_estate_manager.data.db.AppDatabase
import com.example.real_estate_manager.data.db.AttachmentDao
import com.example.real_estate_manager.data.db.PropertyDao
import com.example.real_estate_manager.data.db.PropertyDetailsDao
import com.example.real_estate_manager.data.db.PropertyPhotoDao
import com.example.real_estate_manager.data.db.ProviderWidgetDao
import com.example.real_estate_manager.data.db.TransactionDao
import com.example.real_estate_manager.data.db.UserDao
import com.example.real_estate_manager.data.db.WidgetFieldDao
import com.example.real_estate_manager.data.db.FieldEntryDao
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
                AppDatabase.MIGRATION_5_6
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
}
