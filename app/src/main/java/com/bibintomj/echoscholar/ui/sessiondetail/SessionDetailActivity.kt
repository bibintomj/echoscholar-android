package com.bibintomj.echoscholar.ui.sessiondetail

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bibintomj.echoscholar.data.model.SessionAPIModel
import com.bibintomj.echoscholar.databinding.ActivitySessionDetailBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SessionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionDetailBinding
    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false
    private var isSeeking = false
    private lateinit var updateSeekRunnable: Runnable

    private val handler = Handler(Looper.getMainLooper())
    private var session: SessionAPIModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val json = intent.getStringExtra("session_json")
        Log.d("SessionDetail", "Incoming JSON: $json")

        if (json == null) {
            Toast.makeText(this, "Missing session data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            session = Json.decodeFromString<SessionAPIModel>(json)
        } catch (e: Exception) {
            Log.e("SessionDetail", "Failed to parse JSON", e)
            Toast.makeText(this, "Invalid session data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        session?.let { displaySessionDetails(it) }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    binding.currentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeeking = false
            }
        })
    }

    private fun displaySessionDetails(session: SessionAPIModel) {
        binding.sessionTitle.text = session.transcriptions?.firstOrNull()?.content
            ?.lineSequence()?.firstOrNull()?.trim() ?: "Untitled"

        binding.transcriptionText.text = session.transcriptions?.joinToString("\n\n") { it.content }
            ?: "No transcriptions"

        binding.translationText.text = session.translations?.joinToString("\n\n") { it.content }
            ?: "No translations"

        binding.summaryText.text = session.summaries?.joinToString("\n\n") { it.content }
            ?: "No summaries"

        binding.playAudioButton.setOnClickListener {
            Log.d("SessionDetail", "Audio URL = ${session.audio_signed_url}")
            if (session.audio_signed_url != null) {
                if (!isAudioPlaying) {
                    playAudio(session.audio_signed_url)
                } else {
                    pauseAudio()
                }
            } else {
                Toast.makeText(this, "No audio available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playAudio(url: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
