package com.bibintomj.echoscholar.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bibintomj.echoscholar.SupabaseManager
import com.bibintomj.echoscholar.data.model.SessionAPIModel
import com.bibintomj.echoscholar.data.model.SessionItem
import com.bibintomj.echoscholar.databinding.ActivityDashboardBinding
import com.bibintomj.echoscholar.ui.auth.MainActivity
import com.bibintomj.echoscholar.ui.sessiondetail.SessionDetailActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()
    private var fullSessionList: List<SessionAPIModel> = emptyList()

    private val adapter = SessionAdapter { sessionId ->
        val selectedSession = fullSessionList.find { it.id == sessionId }

        if (selectedSession != null) {
            val json = kotlinx.serialization.json.Json.encodeToString(
                serializer = SessionAPIModel.serializer(),
                value = selectedSession
            )

            Log.d("DashboardActivity", "Session JSON = $json")


            val intent = Intent(this, SessionDetailActivity::class.java)
            intent.putExtra("session_json", json)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        binding.sessionRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sessionRecyclerView.adapter = adapter

        // Load session data
        viewModel.loadSessions()

        viewModel.sessions.observe(this) { sessions ->
            binding.progressBar.visibility = View.GONE

            fullSessionList = sessions

            val items = sessions.map {
                val title = it.transcriptions?.firstOrNull()?.content
                    ?.lineSequence()?.firstOrNull()?.trim()
                    ?: "Untitled"

                SessionItem(
                    id = it.id,
                    title = title,
                    createdOn = it.created_on,
                    userId = it.user_id,
                    targetLanguage = it.target_language,
                    audioFilePath = it.audio_signed_url
                )
            }

            adapter.submitList(items)
        }

        viewModel.error.observe(this) { errorMsg ->
            binding.progressBar.visibility = View.GONE
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // User avatar click shows logout menu
        binding.userAvatar.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menu.add("Logout")
            popupMenu.setOnMenuItemClickListener { item ->
                if (item.title == "Logout") {
                    performLogout()
                    true
                } else false
            }
            popupMenu.show()
        }

        binding.newSessionButton.setOnClickListener {
            val intent = Intent(this, NewSessionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                SupabaseManager.supabase.auth.signOut()

                val intent = Intent(this@DashboardActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@DashboardActivity, "Logout failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
