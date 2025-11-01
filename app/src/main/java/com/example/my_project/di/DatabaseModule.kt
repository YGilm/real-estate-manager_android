package com.example.my_project.di

import android.content.Context
import androidx.room.Room
import com.example.my_project.data.db.AppDatabase
import com.example.my_project.data.db.AttachmentDao
import com.example.my_project.data.db.PropertyDao
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
            // Быстрый способ избежать крэша при смене схемы:
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides
    fun providePropertyDao(db: AppDatabase): PropertyDao = db.propertyDao()
    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()
}