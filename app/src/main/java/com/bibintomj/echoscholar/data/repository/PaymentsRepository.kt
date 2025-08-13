package com.bibintomj.echoscholar.data.repository

import android.util.Log
import com.bibintomj.echoscholar.BuildConfig
import com.bibintomj.echoscholar.data.model.CheckoutRequest
import com.bibintomj.echoscholar.data.model.CheckoutResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PaymentsRepository(
    private val client: OkHttpClient,
    private val supabaseRepo: SupabaseRepository,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
    private val checkoutUrl: String = if (baseUrl.endsWith("/api", ignoreCase = true)) {
        "$baseUrl/stripe/checkout"
    } else {
        "$baseUrl/api/stripe/checkout"
    }

    suspend fun beginCheckout(): Result<String> = withContext(Dispatchers.IO) {
        val token = supabaseRepo.accessTokenOrNull()
            ?: return@withContext Result.failure(IllegalStateException("No Supabase session"))

        val userId = supabaseRepo.currentUserIdOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Missing user id"))

        val email = supabaseRepo.currentUserEmailOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Missing user email"))

        val bodyJson = json.encodeToString(CheckoutRequest(userId = userId, email = email))
        val reqBody = bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val req = Request.Builder()
            .url(checkoutUrl)
            .post(reqBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $token")
            .build()

        try {
            client.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string().orEmpty()
                android.util.Log.d("PaymentsRepository",
                    "Checkout response code=${resp.code}, body=$respBody")

                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${resp.code}: $respBody"))
                }

                val url = try {
                    json.decodeFromString<CheckoutResponse>(respBody).url
                } catch (_: Throwable) {
                    org.json.JSONObject(respBody).optString("url")
                }
                if (url.isBlank()) {
                    return@withContext Result.failure(IllegalStateException("No checkout URL in response"))
                }
                Result.success(url)
            }
        } catch (t: Throwable) {
            Log.e("PaymentsRepository", "Checkout call failed", t)
            Result.failure(t)
        }
    }
}
