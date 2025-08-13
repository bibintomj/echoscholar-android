package com.bibintomj.echoscholar.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bibintomj.echoscholar.BuildConfig
import com.bibintomj.echoscholar.SupabaseManager
import com.bibintomj.echoscholar.databinding.ActivityNewSessionBinding
import com.bibintomj.echoscholar.audio.AudioStreamer
import com.bibintomj.echoscholar.ui.session.SessionUploadViewModel
import com.bibintomj.echoscholar.ui.session.UploadState
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import java.io.File
import android.net.Uri
import com.bibintomj.echoscholar.di.ServiceLocator
import com.bibintomj.echoscholar.ui.session.SessionUploadViewModelFactory


class NewSessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewSessionBinding
    private var isRecording = false
    private var audioStreamer: AudioStreamer? = null

    private val AUDIO_PERMISSION_CODE = 1001

    private val uploadVm: SessionUploadViewModel by viewModels {
        SessionUploadViewModelFactory(
            repository = ServiceLocator.sessionRepository,
            saveSessionUrl = ServiceLocator.saveSessionUrl,
            getAuthToken = {
                // Supabase access token for Authorization header
                SupabaseManager.supabase.auth.currentSessionOrNull()?.accessToken
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeUploadState()

        // Request mic permission on launch
        requestMicrophonePermissionIfNeeded()

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.startRecordingButton.setOnClickListener {
            if (isRecording) {
                stopStreamingAndSave()
            } else {
                // Check again before streaming
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestMicrophonePermissionIfNeeded()
                    return@setOnClickListener
                }
                startStreaming()
            }
        }

        binding.translationLanguage.setOnClickListener {
            Toast.makeText(this, "Language selection not implemented", Toast.LENGTH_SHORT).show()
        }
    }
    private fun observeUploadState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                uploadVm.state.collect { s ->
                    when (s) {
                        is UploadState.Idle -> Unit
                        is UploadState.Loading -> {
                            binding.statusText.text = "Uploading…"
                        }
                        is UploadState.Success -> {
                            binding.statusText.text = "Saved ✓"
                            val data = Intent().putExtra("NEW_SESSION_ID", s.response.sessionId)
                            setResult(Activity.RESULT_OK, data)
                            finish()
                            Toast.makeText(
                                this@NewSessionActivity,
                                "Session saved: ${s.response.sessionId}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is UploadState.Error -> {
                            binding.statusText.text = "Save failed"
                            Toast.makeText(
                                this@NewSessionActivity,
                                "Upload failed: ${s.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
    private fun requestMicrophonePermissionIfNeeded() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startStreaming() {
        val apiKey = BuildConfig.DEEPGRAM_API_KEY // Set this via local.properties
        Log.d("DEBUG", "✅ Deepgram key: ${BuildConfig.DEEPGRAM_API_KEY}")
        val wavOutputFileName = "session-${System.currentTimeMillis()}.wav"
        val wavOutputFile = File(cacheDir, wavOutputFileName)

        audioStreamer = AudioStreamer(
            context = this,
            apiKey = apiKey,
            onTranscription = { transcript ->
                Log.d("NewSessionActivity", "📝 Transcription: $transcript")
                runOnUiThread {
                    binding.transcriptionText.text = transcript
                }
            },
            onTranslation = { translatedText ->
                Log.d("NewSessionActivity", "🌍 Translation: $translatedText")
                runOnUiThread {
                    binding.translationText.text = translatedText
                }
            },
            onError = { error ->
                runOnUiThread {
                    binding.statusText.text = "Error: $error"
                    Toast.makeText(this, "Streaming error: $error", Toast.LENGTH_LONG).show()
                }
            }
        ).apply {
            // NEW: tell the streamer to also record locally to a WAV file
            // (method shown below in the AudioStreamer snippet)
            enableLocalWavRecording(wavOutputFile)
        }


        audioStreamer?.startStreaming()
        isRecording = true

        // Update UI
        binding.startRecordingButton.text = "Stop Recording"
        binding.statusText.text = "● Recording"
        binding.statusText.setTextColor(getColor(android.R.color.holo_red_light))
        binding.transcriptionText.text = "Listening..."
        binding.translationText.text = ""
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
    }


private fun stopStreamingAndSave() {
    // 1) Stop the stream and finalize local WAV
    val audioUri: Uri? = audioStreamer?.stopAndGetRecordedFileUri()
    audioStreamer = null
    isRecording = false

    // 2) Update UI
    binding.startRecordingButton.text = "Start Recording"
    binding.statusText.text = "● Ready"
    binding.statusText.setTextColor(getColor(android.R.color.darker_gray))
    Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()

    // 3) Upload if we have a file
    if (audioUri == null) {
        Toast.makeText(this, "No audio file produced", Toast.LENGTH_LONG).show()
        return
    }

    val transcript = binding.transcriptionText.text?.toString().orEmpty()
    val translation = binding.translationText.text?.toString().orEmpty()

    // Example: pick a target language (replace with your selector)
    val targetLang = "en" // or read from your language selector widget
    val afd = contentResolver.openAssetFileDescriptor(audioUri, "r")
    val size = afd?.length ?: -1
    Log.d("NewSessionActivity", "WAV uri=$audioUri size=$size")
    afd?.close()

    if (size <= 0) {
        Toast.makeText(this, "Empty audio file – nothing to upload", Toast.LENGTH_LONG).show()
        return
    }

    uploadVm.saveSessionFromUri(
        contentResolver = contentResolver,
        audioUri = audioUri,
        transcription = transcript,
        translation = translation,
        targetLanguage = targetLang,
        audioFileName = "audio.wav",
        mimeType = "audio/wav"
    )
}
}
