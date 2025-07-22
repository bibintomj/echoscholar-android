package com.bibintomj.echoscholar.data.repository

import com.bibintomj.echoscholar.SupabaseManager
import com.bibintomj.echoscholar.data.model.SessionAPIModel
import com.bibintomj.echoscholar.data.model.SessionResponse
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection

object SupabaseRepository {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchSessions(): Result<List<SessionAPIModel>> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseManager.supabase.auth.currentAccessTokenOrNull()
                ?: return@withContext Result.failure(Exception("No access token"))

            val request = Request.Builder()
                .url("http://10.192.246.18:3000/api/session") // Emulator -> localhost
                .addHeader("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()

            if (response.code != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(Exception("Error: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

            val parsed = json.decodeFromString<SessionResponse>(body)
            Result.success(parsed.sessions)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
