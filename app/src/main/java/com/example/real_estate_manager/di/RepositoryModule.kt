package com.example.real_estate_manager.di

import com.example.real_estate_manager.data.RealEstateRepository
import com.example.real_estate_manager.data.ReminderRepository
import com.example.real_estate_manager.data.CloudRealEstateRepository
import com.example.real_estate_manager.data.CloudReminderRepository
import com.example.real_estate_manager.data.CloudNotificationRepository
import com.example.real_estate_manager.data.NotificationRepository
import com.example.real_estate_manager.data.CloudStatisticsRepository
import com.example.real_estate_manager.data.StatisticsRepository
import com.example.real_estate_manager.data.RoomRealEstateRepository
import com.example.real_estate_manager.data.RoomReminderRepository
import com.example.real_estate_manager.reminders.ReminderScheduler
import com.example.real_estate_manager.reminders.ReminderWorkScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRepo(impl: CloudRealEstateRepository): RealEstateRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepo(impl: CloudReminderRepository): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepo(impl: CloudNotificationRepository): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepo(impl: CloudStatisticsRepository): StatisticsRepository

    @Binds
    @Singleton
    abstract fun bindReminderWorkScheduler(impl: ReminderScheduler): ReminderWorkScheduler
}
