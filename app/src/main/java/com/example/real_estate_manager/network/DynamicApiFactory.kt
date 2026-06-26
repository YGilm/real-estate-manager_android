package com.example.real_estate_manager.network

import com.example.real_estate_manager.network.storage.ServerConfigStore
import com.google.gson.Gson
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Singleton
class DynamicApiFactory @Inject constructor(
    private val serverConfigStore: ServerConfigStore,
    @Named("noAuthOkHttp") private val noAuthOkHttp: OkHttpClient,
    @Named("authOkHttp") private val authOkHttpProvider: Provider<OkHttpClient>,
    private val gson: Gson
) {
    private data class RetrofitKey(val baseUrl: String, val authenticated: Boolean)

    private val cache = mutableMapOf<RetrofitKey, Retrofit>()

    fun <T : Any> create(apiClass: Class<T>, authenticated: Boolean = true): T {
        val handler = InvocationHandler { _, method, args ->
            val api = retrofit(authenticated).create(apiClass)
            method.invoke(api, *(args ?: emptyArray()))
        }
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(apiClass.classLoader, arrayOf(apiClass), handler) as T
    }

    private fun retrofit(authenticated: Boolean): Retrofit {
        val baseUrl = runBlocking { serverConfigStore.apiBaseUrl() }
        val key = RetrofitKey(baseUrl, authenticated)
        synchronized(cache) {
            return cache.getOrPut(key) {
                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(if (authenticated) authOkHttpProvider.get() else noAuthOkHttp)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
            }
        }
    }
}
