package com.bibintomj.echoscholar.network

import com.bibintomj.echoscholar.SupabaseManager
import io.github.jan.supabase.auth.auth
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ProtectedRequestBuilder(
    private val userIdProvider: () -> String? = {
        SupabaseManager.supabase.auth.currentUserOrNull()?.id
    },
    private val tokenProvider: suspend () -> String? = {
        SupabaseManager.supabase.auth.currentSessionOrNull()?.accessToken
    }
) {
    suspend fun build(from: NetworkRequest): Request {
        val full = (from.baseURL.trimEnd('/') + from.path).toHttpUrl()
            .newBuilder().apply { from.query?.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build()

        val b = Request.Builder().url(full)
        b.header("Content-Type", "application/json")
        from.headers?.forEach { (k, v) -> b.header(k, v) }
        tokenProvider()?.let { b.header("Authorization", "Bearer $it") }
        userIdProvider()?.let { b.header("x-user-id", it) }

        val body = from.bodyJson?.toRequestBody("application/json; charset=utf-8".toMediaType())
        when (from.method.uppercase()) {
            "GET" -> b.get()
            "POST" -> b.post(body ?: "".toRequestBody("application/json".toMediaType()))
            "PUT" -> b.put(body ?: "".toRequestBody("application/json".toMediaType()))
            "DELETE" -> if (body != null) b.delete(body) else b.delete()
            else -> b.method(from.method, body)
        }
        return b.build()
    }
}
