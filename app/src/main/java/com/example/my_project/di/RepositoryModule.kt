package com.example.my_project.di

import com.example.my_project.data.RealEstateRepository
import com.example.my_project.data.RoomRealEstateRepository
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