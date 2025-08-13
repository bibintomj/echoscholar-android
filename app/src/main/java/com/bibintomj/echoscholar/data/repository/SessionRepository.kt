package com.bibintomj.echoscholar.data.repository

import android.util.Log
import com.bibintomj.echoscholar.data.model.SaveSessionRequest
import com.bibintomj.echoscholar.data.model.SaveSessionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

interface SessionRepository {
    suspend fun saveSession(
        saveSessionUrl: String,
        request: SaveSessionRequest,
        authToken: String? = null
    ): SaveSessionResponse

    suspend fun deleteSession(
        id: String,
        authToken: String? = null)
}

class SessionRepositoryImpl(
    private val client: OkHttpClient,
    private val json: Json
) : SessionRepository {

    override suspend fun saveSession(
        saveSessionUrl: String,
        request: SaveSessionRequest,
        authToken: String?
    ): SaveSessionResponse = withContext(Dispatchers.IO) {

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("transcription", request.transcription)
            .addFormDataPart("translation", request.translation)
            .addFormDataPart("target_language", request.targetLanguage)
            .addFormDataPart("audioFileName", request.audioFileName)
            .addFormDataPart("mimeType", request.mimeType)
            .addFormDataPart(
                "file",
                request.audioFileName,
                request.audioBytes.toRequestBody(request.mimeType.toMediaType())
            )
            .build()

        val httpReq = Request.Builder()
            .url(saveSessionUrl)
            .post(multipart)
            .apply {
                header("Accept", "application/json")
                if (!authToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $authToken")
                }
            }
            .build()

        client.newCall(httpReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val allow = resp.header("Allow")
                Log.e("Upload", "HTTP ${resp.code}, Allow=$allow, body=$bodyStr")
                throw IOException("HTTP ${resp.code}: $bodyStr")
            }
            json.decodeFromString(SaveSessionResponse.serializer(), bodyStr)
        }
    }

    override suspend fun deleteSession(
        id: String,
        authToken: String?
    ) = withContext(Dispatchers.IO) {
        // Primary: DELETE /api/session/:id
        val base = com.bibintomj.echoscholar.BuildConfig.API_BASE_URL
        fun reqBuilder(url: String) = Request.Builder()
            .url(url)
            .apply {
                header("Accept", "application/json")
                if (!authToken.isNullOrBlank()) header("Authorization", "Bearer $authToken")
            }

        client.newCall(
            reqBuilder("$base/api/session/$id").delete().build()
        ).execute().use { resp ->
            if (resp.isSuccessful) return@withContext
            Log.w("DeleteSession", "DELETE /api/session/$id -> ${resp.code}")
        }

        // Fallback: POST /api/deletesession  (form field: session_id)
        val form = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("session_id", id)
            .build()

        client.newCall(
            reqBuilder("$base/api/deletesession").post(form).build()
        ).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("Delete failed: HTTP ${resp.code}: $bodyStr")
            }
        }
    }
}
