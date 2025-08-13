package com.bibintomj.echoscholar.data.repository

import com.bibintomj.echoscholar.BuildConfig
import com.bibintomj.echoscholar.SupabaseManager
import com.bibintomj.echoscholar.data.model.SessionAPIModel
import com.bibintomj.echoscholar.data.model.SessionResponse
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection

@Serializable
data class UserSubscription(
    val user_id: String,
    val plan: String
)

object SupabaseRepository {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private fun joinUrl(base: String, path: String): String {
        val b = base.trimEnd('/')
        val p = path.trimStart('/')
        return "$b/$p"
    }

    suspend fun isUserPro(userId: String): Boolean = try {
        SupabaseManager.supabase
            .from("user_subscriptions")
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<UserSubscription>()
            ?.plan == "pro"
    } catch (_: Exception) {
        false
    }

    suspend fun fetchSessions(): Result<List<SessionAPIModel>> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseManager.supabase.auth.currentSessionOrNull()?.accessToken
                ?: return@withContext Result.failure(Exception("No access token"))

            val apiUrl = joinUrl(BuildConfig.API_BASE_URL, "api/session") // ✅ no spaces, no trailing slash

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(Exception("Error: ${response.code}"))
                }

                val body = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                val parsed = json.decodeFromString(SessionResponse.serializer(), body)
                Result.success(parsed.sessions)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // === Add these helpers ===
    suspend fun accessTokenOrNull(): String? =
        SupabaseManager.supabase.auth.currentSessionOrNull()?.accessToken

    fun currentUserIdOrNull(): String? =
        SupabaseManager.supabase.auth.currentUserOrNull()?.id

    fun currentUserEmailOrNull(): String? =
        SupabaseManager.supabase.auth.currentUserOrNull()?.email
// =========================

}
