package com.bibintomj.echoscholar.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.bibintomj.echoscholar.services.GoogleTranslateService
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class AudioStreamer(
    private val context: Context,
    private val apiKey: String,
    private val onTranscription: (String) -> Unit,
    private val onTranslation: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val targetLanguage: String = "es"
) {
    private var webSocket: WebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val client = OkHttpClient()
    private val fullTranscript = StringBuilder()
    private val fullTranslation = StringBuilder()

    private var localWavFile: File? = null
    private var wavWriter: WavFileWriter? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    fun enableLocalWavRecording(outputFile: File) {
        localWavFile = outputFile
        wavWriter = WavFileWriter(outputFile)
    }

    fun startStreaming() {
        val url =
            "wss://api.deepgram.com/v1/listen?encoding=linear16&sample_rate=16000&channels=1&model=nova&smart_format=true&filler_words=true"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Token $apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("AudioStreamer", "🔗 WebSocket connected to Deepgram")
                startRecording()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("AudioStreamer", "📥 Received message: $text")

                try {
                    val json = JSONObject(text)
                    val transcript = json
                        .optJSONObject("channel")
                        ?.optJSONArray("alternatives")
                        ?.optJSONObject(0)
                        ?.optString("transcript")

                    if (!transcript.isNullOrBlank()) {
                        fullTranscript.append(transcript).append(" ")
                        val fullText = fullTranscript.toString().trim()

                        // ✅ Emit transcription to UI
                        onTranscription(fullText)

                        // ✅ Translate the latest text and append
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val translated = GoogleTranslateService.translateText(
                                    text = transcript,
                                    targetLanguage = targetLanguage
                                )
                                translated?.let {
                                    fullTranslation.append(it).append(" ")

                                    withContext(Dispatchers.Main) {
                                        onTranslation(fullTranslation.toString().trim())
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("AudioStreamer", "❌ Translation failed: ${e.message}")
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("AudioStreamer", "❌ Parsing error", e)
                    onError("Failed to parse Deepgram response")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("AudioStreamer", "❌ WebSocket error: ${t.message}")
                onError("WebSocket error: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("AudioStreamer", "🔌 WebSocket closed")
            }
        })
    }

    private fun startRecording() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onError("Microphone permission not granted")
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            onError("AudioRecord not initialized")
            return
        }

        audioRecord?.startRecording()
        Log.d("AudioStreamer", "🎙️ Started recording")

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    Log.d("AudioStreamer", "📤 Sending $read bytes")
                    webSocket?.send(buffer.toByteString(0, read))
                    wavWriter?.writePcm(buffer, read)
                }
            }
        }
    }

    fun stopAndGetRecordedFileUri(): Uri? {
        try {
            stopStreaming() // your existing stop logic (stop AudioRecord/ws)
            wavWriter?.finish()
            val f = localWavFile ?: return null
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                f
            )
        } finally {
            wavWriter = null
            localWavFile = null
        }
    }

    fun stopStreaming() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        webSocket?.close(1000, "Stopped by user")
        webSocket = null
        fullTranscript.clear()
        fullTranslation.clear()
        Log.d("AudioStreamer", "🛑 Stopped streaming")
    }
}
