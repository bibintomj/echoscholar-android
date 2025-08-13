package com.bibintomj.echoscholar.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.bibintomj.echoscholar.BuildConfig

object GoogleTranslateService {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun translateText(
        text: String,
        targetLanguage: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("q", text)
                put("target", targetLanguage)
                put("format", "text")
                put("key", BuildConfig.GOOGLE_TRANSLATE_API_KEY)
            }

            val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://translation.googleapis.com/language/translate/v2?key=AIzaSyAgILAmQupAdmZ2yXaBD0IR7A8asXnNDMw")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("Translator", "🔁 Google Response: $responseBody")

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                return@withContext "Translation failed: ${response.code}"
            }

            val root = JSONObject(responseBody)
            val translatedText = root
                .getJSONObject("data")
                .getJSONArray("translations")
                .getJSONObject(0)
                .getString("translatedText")

            return@withContext translatedText
        } catch (e: Exception) {
            Log.e("Translator", "❌ Translation error", e)
            return@withContext "Translation error: ${e.message}"
        }
    }
}
