package com.bibintomj.echoscholar.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bibintomj.echoscholar.SupabaseManager
import com.bibintomj.echoscholar.databinding.ActivityNewSessionBinding
import com.bibintomj.echoscholar.audio.AudioStreamer
import io.github.jan.supabase.auth.auth
import okhttp3.WebSocket

class NewSessionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewSessionBinding
    private var isRecording = false
    private var audioStreamer: AudioStreamer? = null

    private val AUDIO_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request mic permission on launch
        requestMicrophonePermissionIfNeeded()

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.startRecordingButton.setOnClickListener {
            if (isRecording) {
                stopStreaming()
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
        val socketUrl = "ws://10.0.2.2:8080"
        val userId = SupabaseManager.supabase.auth.currentUserOrNull()?.id
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val language = "es" // Replace with selected language later

        audioStreamer = AudioStreamer(
            context = this,
            socketUrl = socketUrl,
            userId = userId,
            language = language,
            onMessage = { transcription, translation ->
                Log.d("NewSessionActivity", "UI Update -> transcription: $transcription, translation: $translation")
                runOnUiThread {
                    binding.transcriptionText.text = transcription
                    binding.translationText.text = translation
                }
            },
            onError = { error ->
                runOnUiThread {
                    binding.statusText.text = "Error: $error"
                    Toast.makeText(this, "Streaming error: $error", Toast.LENGTH_LONG).show()
                }
            }
        )

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

    private fun stopStreaming() {
        audioStreamer?.stopStreaming()
        isRecording = false

        // Update UI
        binding.startRecordingButton.text = "Start Recording"
        binding.statusText.text = "● Ready"
        binding.statusText.setTextColor(getColor(android.R.color.darker_gray))
//        binding.transcriptionText.text = "This is a sample transcription of what you said."
//        binding.translationText.text = "Esta es una traducción de muestra."
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
    }
}
