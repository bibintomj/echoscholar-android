package com.bibintomj.echoscholar.repository

import com.bibintomj.echoscholar.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class AuthRepository {

    private val auth = SupabaseManager.supabase.auth
    private val client = SupabaseManager.supabase.httpClient
    private val supabaseUrl = SupabaseManager.supabase.supabaseUrl

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
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
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogleIdToken(idToken: String): Result<Unit> {
        return try {
            val response: HttpResponse = client.post("$supabaseUrl/auth/v1/token?grant_type=id_token") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "provider" to "google",
                    "id_token" to idToken
                ))
            }

            if (response.status.isSuccess()) {
                val session: SupabaseSessionResponse = response.body()
//                auth.saveSession(session)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Google login failed: ${response.status}"))
            }
        } catch (e: Exception) {
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