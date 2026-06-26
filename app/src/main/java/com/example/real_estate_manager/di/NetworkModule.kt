package com.example.real_estate_manager.di

import com.example.real_estate_manager.network.api.AuthApi
import com.example.real_estate_manager.network.api.CustomProviderFieldApi
import com.example.real_estate_manager.network.api.HealthApi
import com.example.real_estate_manager.network.api.MeterReadingApi
import com.example.real_estate_manager.network.api.NotificationApi
import com.example.real_estate_manager.network.api.PropertyApi
import com.example.real_estate_manager.network.api.PropertyDocumentApi
import com.example.real_estate_manager.network.api.PropertyPhotoApi
import com.example.real_estate_manager.network.api.ReminderApi
import com.example.real_estate_manager.network.api.StatisticsApi
import com.example.real_estate_manager.network.api.TransactionApi
import com.example.real_estate_manager.network.api.UtilityProviderApi
import com.example.real_estate_manager.network.DynamicApiFactory
import com.example.real_estate_manager.network.auth.JwtAuthenticator
import com.example.real_estate_manager.network.interceptors.JwtAuthInterceptor
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideGson(): Gson =
        GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .create()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Provides
    @Singleton
    @Named("noAuthOkHttp")
    fun provideNoAuthOkHttp(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    @Named("authOkHttp")
    fun provideAuthOkHttp(
        jwtAuthInterceptor: JwtAuthInterceptor,
        jwtAuthenticator: JwtAuthenticator,
        logging: HttpLoggingInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(jwtAuthInterceptor)
            .authenticator(jwtAuthenticator)
            .addInterceptor(logging)
            .build()

    @Provides
    @Singleton
    @Named("noAuthAuthApi")
    fun provideNoAuthAuthApi(factory: DynamicApiFactory): AuthApi =
        factory.create(AuthApi::class.java, authenticated = false)

    @Provides
    @Singleton
    fun provideAuthApi(factory: DynamicApiFactory): AuthApi =
        factory.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideHealthApi(factory: DynamicApiFactory): HealthApi =
        factory.create(HealthApi::class.java, authenticated = false)

    @Provides
    @Singleton
    fun providePropertyApi(factory: DynamicApiFactory): PropertyApi =
        factory.create(PropertyApi::class.java)

    @Provides
    @Singleton
    fun provideTransactionApi(factory: DynamicApiFactory): TransactionApi =
        factory.create(TransactionApi::class.java)

    @Provides
    @Singleton
    fun provideReminderApi(factory: DynamicApiFactory): ReminderApi =
        factory.create(ReminderApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(factory: DynamicApiFactory): NotificationApi =
        factory.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideStatisticsApi(factory: DynamicApiFactory): StatisticsApi =
        factory.create(StatisticsApi::class.java)

    @Provides
    @Singleton
    fun providePropertyPhotoApi(factory: DynamicApiFactory): PropertyPhotoApi =
        factory.create(PropertyPhotoApi::class.java)

    @Provides
    @Singleton
    fun providePropertyDocumentApi(factory: DynamicApiFactory): PropertyDocumentApi =
        factory.create(PropertyDocumentApi::class.java)

    @Provides
    @Singleton
    fun provideUtilityProviderApi(factory: DynamicApiFactory): UtilityProviderApi =
        factory.create(UtilityProviderApi::class.java)

    @Provides
    @Singleton
    fun provideCustomProviderFieldApi(factory: DynamicApiFactory): CustomProviderFieldApi =
        factory.create(CustomProviderFieldApi::class.java)

    @Provides
    @Singleton
    fun provideMeterReadingApi(factory: DynamicApiFactory): MeterReadingApi =
        factory.create(MeterReadingApi::class.java)
}
