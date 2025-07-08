package com.bibintomj.echoscholar.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject

class AudioStreamer(
    private val context: Context,
    private val socketUrl: String,
    private val userId: String,
    private val language: String,
    private val onMessage: (transcription: String, translation: String)  -> Unit,
    private val onError: (String) -> Unit
) {
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val client = OkHttpClient()

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun startStreaming() {
        val request = Request.Builder().url(socketUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d("AudioStreamer", "WebSocket opened")
                sendInitMessage()
                startRecording()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("AudioStreamer", "Message received: $text")
                try {
                    val json = JSONObject(text)
                    val transcription = json.optString("transcription")
                    val translation = json.optString("translation")

                    if (transcription.isNotEmpty() || translation.isNotEmpty()) {
                        onMessage(transcription, translation)
                    }
                } catch (e: Exception) {
                    Log.e("AudioStreamer", "Error parsing message", e)
                    onError("Parsing error")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: okhttp3.Response?) {
                onError("WebSocket failed: ${t.message}")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d("AudioStreamer", "WebSocket closed")
            }

        })
    }

    private fun sendInitMessage() {
        val initMessage = """{"lang":"$language","userId":"$userId"}"""
        webSocket?.send(initMessage)
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onError("Microphone permission not granted")
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    webSocket?.send(buffer.toByteString(0, read))
                }
            }
        }
    }

    fun stopStreaming() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        webSocket?.close(1000, "User stopped recording")
        webSocket = null
    }


}
