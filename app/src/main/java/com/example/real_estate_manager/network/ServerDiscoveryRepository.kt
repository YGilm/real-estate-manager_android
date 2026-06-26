package com.example.real_estate_manager.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.real_estate_manager.network.storage.ServerConfigStore
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class ServerDiscoveryResult(
    val foundUrl: String?,
    val message: String,
    val checkedUrls: List<String>
) {
    val found: Boolean get() = foundUrl != null
}

@Singleton
class ServerDiscoveryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("noAuthOkHttp") okHttpClient: OkHttpClient
) {
    private val client = okHttpClient.newBuilder()
        .connectTimeout(450, TimeUnit.MILLISECONDS)
        .readTimeout(700, TimeUnit.MILLISECONDS)
        .writeTimeout(700, TimeUnit.MILLISECONDS)
        .callTimeout(900, TimeUnit.MILLISECONDS)
        .build()

    suspend fun discover(savedServerUrl: String): ServerDiscoveryResult = withContext(Dispatchers.IO) {
        val checked = mutableListOf<String>()

        checkCandidate(savedServerUrl)?.let { return@withContext it.withChecked(checked + savedServerUrl) }
        checked += savedServerUrl

        val localCandidates = discoverMdnsCandidates()
        checkCandidates(localCandidates, checked)?.let { return@withContext it }

        val subnetCandidates = subnetCandidates()
        checkCandidates(subnetCandidates, checked)?.let { return@withContext it }

        ServerDiscoveryResult(
            foundUrl = null,
            message = if (checked.isEmpty()) {
                "Сервер не найден: нет доступной локальной IPv4-сети"
            } else {
                "Сервер не найден. Проверено адресов: ${checked.size}. Последний адрес: ${checked.last()}"
            },
            checkedUrls = checked
        )
    }

    private suspend fun checkCandidates(
        candidates: List<String>,
        checked: MutableList<String>
    ): ServerDiscoveryResult? {
        if (candidates.isEmpty()) return null
        checked += candidates
        val found = coroutineScope {
            candidates
                .chunked(MAX_PARALLEL_CHECKS)
                .firstNotNullOfOrNull { chunk ->
                    chunk.map { candidate ->
                        async { checkCandidate(candidate) }
                    }.awaitAll().firstOrNull { it?.found == true }
                }
        }
        return found?.withChecked(checked)
    }

    private fun checkCandidate(serverUrl: String): ServerDiscoveryResult? {
        val normalized = normalizeServerUrl(serverUrl)
        val request = Request.Builder()
            .url("${normalized}api/health/")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "health failed url=$normalized code=${response.code}")
                    null
                } else {
                    val body = response.body?.string().orEmpty()
                    val status = runCatching {
                        JsonParser.parseString(body).asJsonObject.get("status")?.asString
                    }.getOrNull()
                    if (status == "ok") {
                        ServerDiscoveryResult(
                            foundUrl = normalized,
                            message = "Автоматически найден сервер: $normalized",
                            checkedUrls = emptyList()
                        )
                    } else {
                        Log.d(TAG, "health returned unexpected status url=$normalized status=$status")
                        null
                    }
                }
            }
        } catch (error: SocketTimeoutException) {
            Log.d(TAG, "health timeout url=$normalized")
            null
        } catch (error: IOException) {
            Log.d(TAG, "health unavailable url=$normalized message=${error.message}")
            null
        } catch (error: Throwable) {
            Log.d(TAG, "health failed url=$normalized error=${error::class.java.simpleName}: ${error.message}")
            null
        }
    }

    private suspend fun discoverMdnsCandidates(): List<String> = withContext(Dispatchers.IO) {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        val candidates = linkedSetOf<String>()
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.port != 0 && serviceInfo.port != ServerConfigStore.DEFAULT_SERVER_PORT) return
                runCatching {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val host = serviceInfo.host?.hostAddress ?: serviceInfo.serviceName
                            val port = serviceInfo.port.takeIf { it > 0 } ?: ServerConfigStore.DEFAULT_SERVER_PORT
                            synchronized(candidates) {
                                candidates += "http://$host:$port/"
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        val started = runCatching {
            nsdManager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }.isSuccess
        if (started) {
            delay(MDNS_DISCOVERY_TIMEOUT_MS)
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        }
        synchronized(candidates) { candidates.toList() }
    }

    private fun subnetCandidates(): List<String> {
        val localAddress = localIpv4Address() ?: return emptyList()
        val prefix = localAddress.substringBeforeLast('.', missingDelimiterValue = "")
        if (prefix.isBlank()) return emptyList()
        val localHost = localAddress.substringAfterLast('.').toIntOrNull()
        val likelyHosts = buildList {
            add(1)
            addAll(100..110)
            if (localHost != null) addAll((localHost - 8)..(localHost + 8))
            addAll(2..99)
            addAll(111..254)
        }
        return likelyHosts
            .filter { it in 1..254 && it != localHost }
            .distinct()
            .map { host -> "http://$prefix.$host:${ServerConfigStore.DEFAULT_SERVER_PORT}/" }
    }

    private fun localIpv4Address(): String? =
        NetworkInterface.getNetworkInterfaces().toList()
            .asSequence()
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { iface ->
                when {
                    iface.name.startsWith("wlan", ignoreCase = true) -> 0
                    iface.name.startsWith("eth", ignoreCase = true) -> 1
                    else -> 2
                }
            }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress

    private fun normalizeServerUrl(raw: String): String {
        val trimmed = raw.trim().ifBlank { ServerConfigStore.DEBUG_SERVER_URL }
        val withoutApi = trimmed
            .removeSuffix("/")
            .removeSuffix("/api")
            .removeSuffix("/api/")
        return "$withoutApi/"
    }

    private fun ServerDiscoveryResult.withChecked(checked: List<String>): ServerDiscoveryResult =
        copy(checkedUrls = checked.distinct())

    private companion object {
        const val TAG = "ServerDiscovery"
        const val NSD_SERVICE_TYPE = "_http._tcp."
        const val MDNS_DISCOVERY_TIMEOUT_MS = 2_500L
        const val MAX_PARALLEL_CHECKS = 24
    }
}
