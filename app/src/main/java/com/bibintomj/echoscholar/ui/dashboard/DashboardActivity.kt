package com.bibintomj.echoscholar.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bibintomj.echoscholar.R
import com.bibintomj.echoscholar.SupabaseManager
import com.bibintomj.echoscholar.data.model.SessionAPIModel
import com.bibintomj.echoscholar.data.model.SessionItem
import com.bibintomj.echoscholar.data.repository.SupabaseRepository
import com.bibintomj.echoscholar.databinding.ActivityDashboardBinding
import com.bibintomj.echoscholar.ui.auth.MainActivity
import com.bibintomj.echoscholar.ui.chat.ChatActivity
import com.bibintomj.echoscholar.ui.payments.PricingActivity
import com.bibintomj.echoscholar.ui.sessiondetail.SessionDetailActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()
    private var fullSessionList: List<SessionAPIModel> = emptyList()
    private var pendingHighlightId: String? = null

    private var actionMode: ActionMode? = null

    private val selectionModeCb: ActionMode.Callback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(com.bibintomj.echoscholar.R.menu.menu_selection, menu)
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return if (item.itemId == com.bibintomj.echoscholar.R.id.action_delete) {
                confirmBulkDelete(mode)   // ← no adapter used here
                true
            } else false
        }
        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            adapter.exitSelection()
        }
    }

    private fun confirmBulkDelete(mode: ActionMode) {
        val ids: List<String> = adapter.getSelectedIds()
        AlertDialog.Builder(this)
            .setTitle("Delete ${ids.size} sessions?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                ids.forEach { id -> viewModel.deleteSession(id) }
                mode.finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private val newSessionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newId = result.data?.getStringExtra("NEW_SESSION_ID")
            pendingHighlightId = newId
            binding.progressBar.visibility = View.VISIBLE
            runCatching { binding.swipeRefresh.isRefreshing = false }
            viewModel.loadSessions()
        }
    }

    // NOTE: SessionAdapter now requires two callbacks.
    private val adapter = SessionAdapter(
        onSessionClick = { openSession(it) },
        onSelectionChanged = { count ->
            if (count > 0 && actionMode == null) {
                actionMode = startSupportActionMode(selectionModeCb)
            }
            actionMode?.title = "$count selected"
            if (count == 0) actionMode?.finish()
        }
    )

    private val pricingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                val user = SupabaseManager.supabase.auth.currentUserOrNull() ?: return@launch
                val isPro = runCatching { SupabaseRepository.isUserPro(user.id) }
                    .getOrElse { false }
                if (isPro) {
                    // Optional: take them straight to Chat if now Pro
                    startActivity(Intent(this@DashboardActivity, ChatActivity::class.java))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView
        binding.sessionRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.sessionRecyclerView.adapter = adapter

        // Swipe-to-delete (disabled while in selection mode)
        val swipeToDelete = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                if (adapter.selectionMode) {
                    adapter.notifyItemChanged(vh.bindingAdapterPosition)
                    return
                }
                val position = vh.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position)
                if (item == null) {
                    adapter.notifyItemChanged(position)
                    return
                }
                AlertDialog.Builder(this@DashboardActivity)
                    .setTitle("Delete session?")
                    .setMessage("This will permanently delete the session.")
                    .setPositiveButton("Delete") { _, _ ->
                        binding.progressBar.visibility = View.VISIBLE
                        viewModel.deleteSession(item.id)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }
        ItemTouchHelper(swipeToDelete).attachToRecyclerView(binding.sessionRecyclerView)

        // Pull-to-refresh if present
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadSessions()
        }

        // Initial load
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadSessions()

        // Observe sessions
        viewModel.sessions.observe(this) { sessions ->
            runCatching { binding.swipeRefresh.isRefreshing = false }
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

            val itemsSorted = items.sortedByDescending { it.createdOn }
            adapter.submitList(itemsSorted) {
                pendingHighlightId?.let { id ->
                    val idx = itemsSorted.indexOfFirst { it.id == id }
                    if (idx >= 0) binding.sessionRecyclerView.smoothScrollToPosition(idx)
                    pendingHighlightId = null
                }
            }
        }

        // Observe errors
        viewModel.error.observe(this) { errorMsg ->
            runCatching { binding.swipeRefresh.isRefreshing = false }
            binding.progressBar.visibility = View.GONE
            errorMsg?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }

        binding.chatButton.setOnClickListener {
            val user = SupabaseManager.supabase.auth.currentUserOrNull()
            if (user == null) {
                Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val isPro = try {
                    SupabaseRepository.isUserPro(user.id)
                } catch (e: Exception) {
                    Log.e("DashboardActivity", "Pro check failed", e)
                    false
                }

                if (!isPro) {
                    // If not subscribed, go to PricingActivity
                    pricingLauncher.launch(Intent(this@DashboardActivity, PricingActivity::class.java))
                    return@launch
                }

                // If subscribed, open ChatActivity
                startActivity(Intent(this@DashboardActivity, ChatActivity::class.java))
            }
        }



        // Avatar menu
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

        // New Session
        binding.newSessionButton.setOnClickListener {
            newSessionLauncher.launch(Intent(this, NewSessionActivity::class.java))
        }
    }



    // ——— helpers ———
    private fun openSession(sessionId: String) {
        val selectedSession = fullSessionList.find { it.id == sessionId }
        if (selectedSession != null) {
            val json = Json.encodeToString(SessionAPIModel.serializer(), selectedSession)
            Log.d("DashboardActivity", "Session JSON = $json")
            val intent = Intent(this, SessionDetailActivity::class.java)
            intent.putExtra("session_json", json)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show()
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
