package com.bibintomj.echoscholar.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okio.BufferedSource

class NetworkClient(
    private val client: OkHttpClient,
    private val requestBuilder: ProtectedRequestBuilder
) {
    suspend fun <T> stream(
        request: NetworkRequest,
        linePrefix: String = "0:",
        terminator: String = "d:",
        map: (String) -> T,
        onElement: (T) -> Unit
    ) {
        val req = requestBuilder.build(request)
        withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string()
                    throw IllegalStateException("HTTP ${resp.code}: ${err ?: resp.message}")
                }
                val source: BufferedSource = resp.body?.source()
                    ?: throw IllegalStateException("Empty response body")

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    val trimmed = line.trim()
                    if (trimmed.startsWith(terminator)) break
                    if (trimmed.startsWith(linePrefix)) {
                        val value = trimmed.removePrefix(linePrefix)
                        onElement(map(value))
                    }
                }
            }
        }
    }
}
