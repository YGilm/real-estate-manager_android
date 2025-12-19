package com.example.my_project.di

import android.content.Context
import androidx.room.Room
import com.example.my_project.data.db.AppDatabase
import com.example.my_project.data.db.AttachmentDao
import com.example.my_project.data.db.PropertyDao
import com.example.my_project.data.db.PropertyDetailsDao
import com.example.my_project.data.db.PropertyPhotoDao
import com.example.my_project.data.db.TransactionDao
import com.example.my_project.data.db.UserDao
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
            // ❗️Важно: НЕ используем fallbackToDestructiveMigration(),
            // чтобы не терять данные пользователей при обновлениях схемы.
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun providePropertyDao(db: AppDatabase): PropertyDao = db.propertyDao()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    fun providePropertyDetailsDao(db: AppDatabase): PropertyDetailsDao = db.propertyDetailsDao()

    @Provides
    fun providePropertyPhotoDao(db: AppDatabase): PropertyPhotoDao = db.propertyPhotoDao()
}