package com.bibintomj.echoscholar.ui.sessiondetail

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bibintomj.echoscholar.BuildConfig
import com.bibintomj.echoscholar.SupabaseManager
import com.bibintomj.echoscholar.data.model.SessionAPIModel
import com.bibintomj.echoscholar.data.repository.SupabaseRepository
import com.bibintomj.echoscholar.databinding.ActivitySessionDetailBinding
import com.bibintomj.echoscholar.ui.chat.ChatActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class SessionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionDetailBinding
    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false
    private var isSeeking = false
    private lateinit var updateSeekRunnable: Runnable
    private val handler = Handler(Looper.getMainLooper())

    private var session: SessionAPIModel? = null
    private val http by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val json = intent.getStringExtra("session_json")
        Log.d("SessionDetail", "Incoming JSON: $json")

        if (json.isNullOrBlank()) {
            Toast.makeText(this, "Missing session data", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        try {
            session = Json.decodeFromString(SessionAPIModel.serializer(), json)
        } catch (e: Exception) {
            Log.e("SessionDetail", "Failed to parse JSON", e)
            Toast.makeText(this, "Invalid session data", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        session?.let { displaySessionDetails(it) }

        binding.backButton.setOnClickListener { finish() }

        binding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    binding.currentTime.text = formatTime(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { isSeeking = false }
        })

        binding.generateMomButton.setOnClickListener {
            session?.id?.let { generateMoM(it) }
        }

        // ✅ Open Chat with session context (Pro-only)
        binding.chatButton.setOnClickListener {
            val user = SupabaseManager.supabase.auth.currentUserOrNull()
            if (user == null) {
                Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val sessionId = session?.id
            if (sessionId.isNullOrBlank()) {
                Toast.makeText(this, "Session ID not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val isPro = try {
                    SupabaseRepository.isUserPro(user.id)
                } catch (e: Exception) {
                    Log.e("SessionDetail", "Pro check failed", e)
                    false
                }

                if (!isPro) {
                    Toast.makeText(this@SessionDetailActivity, "Upgrade to Pro to use Chat", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val intent = Intent(this@SessionDetailActivity, ChatActivity::class.java)
                    .putExtra("session_id", sessionId)
                startActivity(intent)
            }
        }
    }

    private fun displaySessionDetails(session: SessionAPIModel) {
        binding.sessionTitle.text = session.transcriptions?.firstOrNull()?.content
            ?.lineSequence()?.firstOrNull()?.trim().orEmpty().ifBlank { "Untitled" }

        binding.transcriptionText.text =
            session.transcriptions?.joinToString("\n\n") { it.content }.orEmpty()
                .ifBlank { "No transcriptions" }

        binding.translationText.text =
            session.translations?.joinToString("\n\n") { it.content }.orEmpty()
                .ifBlank { "No translations" }

        binding.summaryText.text =
            session.summaries?.joinToString("\n\n") { it.content }.orEmpty()
                .ifBlank { "No summaries" }

        binding.playAudioButton.setOnClickListener {
            val url = session.audio_signed_url
            Log.d("SessionDetail", "Audio URL = $url")
            if (url.isNullOrBlank()) {
                Toast.makeText(this, "No audio available", Toast.LENGTH_SHORT).show()
            } else {
                if (!isAudioPlaying) playAudio(url) else pauseAudio()
            }
        }
    }

    private fun playAudio(url: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                it.start()
                isAudioPlaying = true
                binding.playAudioButton.setImageResource(android.R.drawable.ic_media_pause)

                val totalDuration = it.duration
                binding.totalTime.text = formatTime(totalDuration)
                binding.audioSeekBar.max = totalDuration

                startSeekBarUpdates()
            }
            setOnCompletionListener {
                isAudioPlaying = false
                binding.playAudioButton.setImageResource(android.R.drawable.ic_media_play)
                handler.removeCallbacks(updateSeekRunnable)
                binding.audioSeekBar.progress = 0
                binding.currentTime.text = "0:00"
            }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@SessionDetailActivity, "Error playing audio", Toast.LENGTH_SHORT).show()
                false
            }
            prepareAsync()
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.pause()
        isAudioPlaying = false
        binding.playAudioButton.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun startSeekBarUpdates() {
        updateSeekRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    val current = it.currentPosition
                    if (!isSeeking) {
                        binding.audioSeekBar.progress = current
                        binding.currentTime.text = formatTime(current)
                    }
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(updateSeekRunnable)
    }

    private fun formatTime(ms: Int): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%d:%02d", mins, secs)
    }

    private fun joinUrl(base: String, path: String): String {
        val b = base.trimEnd('/')
        val p = path.trimStart('/')
        return "$b/$p"
    }

    private fun generateMoM(sessionId: String) {
        val accessToken = SupabaseManager.supabase.auth.currentAccessTokenOrNull()
        if (accessToken.isNullOrBlank()) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val apiUrl = joinUrl(BuildConfig.API_BASE_URL, "api/mom") // ✅ robust join

        val jsonBody = """{"session_id":"$sessionId"}"""
        val mediaType = "application/json".toMediaType()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(mediaType))
            .build()

        http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SessionDetailActivity, "Failed to fetch MoM", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    if (!response.isSuccessful || body.isNullOrBlank()) {
                        Toast.makeText(this@SessionDetailActivity, "Error: ${response.code}", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            val mom = Json.parseToJsonElement(body).jsonObject["mom"]?.jsonPrimitive?.content
                                ?: "No MoM found"
                            binding.momText.text = mom
                        } catch (_: Exception) {
                            binding.momText.text = "Error parsing MoM"
                        }
                    }
                }
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
