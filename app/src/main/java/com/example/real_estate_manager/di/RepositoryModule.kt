package com.example.real_estate_manager.di

import com.example.real_estate_manager.data.RealEstateRepository
import com.example.real_estate_manager.data.RoomRealEstateRepository
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
    abstract fun bindRepo(impl: RoomRealEstateRepository): RealEstateRepository
}