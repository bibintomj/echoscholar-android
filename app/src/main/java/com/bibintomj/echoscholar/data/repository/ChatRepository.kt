package com.bibintomj.echoscholar.data.repository

import android.util.Log
import com.bibintomj.echoscholar.data.model.ChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ChatRepository(
    private val baseUrl: String,
    private val authTokenProvider: (() -> String?)? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // keep streaming open
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val TAG = "ChatRepository"

    fun streamChat(request: ChatRequest) = callbackFlow<String> {
        val url = baseUrl.trimEnd('/') + "/api/chat"
        Log.d(TAG, "POST $url")

        val bodyString = json.encodeToString(request)
        val body = bodyString.toRequestBody("application/json".toMediaType())

        val reqBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .header("Connection", "keep-alive")

        authTokenProvider?.invoke()?.let { token ->
            if (!token.isNullOrBlank()) reqBuilder.header("Authorization", "Bearer $token")
        }

        val call = client.newCall(reqBuilder.build())

        val job = launch(Dispatchers.IO) {
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    val err = try { response.body?.string().orEmpty() } catch (_: Throwable) { "" }
                    Log.e(TAG, "HTTP ${response.code} - $err")
                    trySend("[error] HTTP ${response.code}: $err").isSuccess
                    return@launch
                }

                response.body?.byteStream().use { stream ->
                    if (stream == null) {
                        trySend("[error] empty body").isSuccess
                        return@launch
                    }

                    BufferedReader(InputStreamReader(stream)).use { r ->
                        while (true) {
                            val rawLine = r.readLine() ?: break
                            val trimmed = rawLine.trim()
                            if (trimmed.isEmpty()) continue

                            // Support SSE "data: ..." lines
                            val data = if (trimmed.startsWith("data:", ignoreCase = true))
                                trimmed.substringAfter("data:").trim()
                            else trimmed

                            // Termination markers
                            if (data == "[DONE]" || data == "d:" || data.startsWith("d:")) {
                                trySend("[done]").isSuccess
                                break
                            }

                            // iOS-style tokens: 0:"...”
                            if (data.startsWith("0:")) {
                                val value = data.removePrefix("0:").trim()
                                val token = value.removeSurrounding("\"")
                                if (token.isNotEmpty()) trySend(token).isSuccess
                                continue
                            }

                            // JSON token shapes
                            if (data.startsWith("{") && data.endsWith("}")) {
                                extractTokenFromJson(data)?.let { t ->
                                    if (t.isNotEmpty()) trySend(t).isSuccess
                                } ?: run {
                                    // Uncomment to inspect unknown JSON
                                    // Log.d(TAG, "Unrecognized JSON: $data")
                                }
                                continue
                            }

                            // Fallback: plain text line
                            trySend(data).isSuccess
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Streaming error", t)
                trySend("[error] ${t.javaClass.simpleName}: ${t.message}").isSuccess
            } finally {
                this@callbackFlow.close()
            }
        }

        awaitClose {
            try { call.cancel() } catch (_: Exception) {}
            job.cancel()
        }
    }

    private fun extractTokenFromJson(objLine: String): String? {
        return try {
            val el = json.parseToJsonElement(objLine)
            if (el !is JsonObject) return null

            // Try common fields in order
            el["0"]?.jsonPrimitive?.contentOrNull
                ?: el["delta"]?.jsonPrimitive?.contentOrNull
                ?: el["token"]?.jsonPrimitive?.contentOrNull
                ?: el["text"]?.jsonPrimitive?.contentOrNull
                ?: el["content"]?.jsonPrimitive?.contentOrNull
                // OpenAI-like: { choices: [ { delta: { content: "..." } } ] }
                ?: el["choices"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("delta")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.contentOrNull
        } catch (_: Throwable) {
            null
        }
    }
}
