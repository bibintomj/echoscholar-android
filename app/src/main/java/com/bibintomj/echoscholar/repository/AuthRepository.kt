package com.bibintomj.echoscholar.repository

import android.util.Log
import com.bibintomj.echoscholar.BuildConfig
import com.bibintomj.echoscholar.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.Serializable

class AuthRepository {

    private val auth = SupabaseManager.supabase.auth
    private val client = SupabaseManager.supabase.httpClient
    private val supabaseUrl = SupabaseManager.supabase.supabaseUrl.trimEnd('/')

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login error", e)
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Register error", e)
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogleIdToken(idToken: String): Result<Unit> {
        return try {
            val url = "https://$supabaseUrl/auth/v1/token?grant_type=id_token"
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                    append("apikey", BuildConfig.SUPABASE_ANON_KEY)
                }
                setBody(
                    mapOf(
                        "provider" to "google",
                        "id_token" to idToken
                    )
                )
            }

            val sessionText = response.bodyAsText()
            Log.d("GoogleSession", sessionText)

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Google login failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google login failed", e)
            Result.failure(e)
        }
    }

    @Serializable
    data class SupabaseSessionResponse(
        val access_token: String,
        val refresh_token: String,
        val token_type: String,
        val expires_in: Int,
        val user: Map<String, Any?>
    )
}
