package com.bibintomj.echoscholar.ui.chat

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bibintomj.echoscholar.BuildConfig
import com.bibintomj.echoscholar.R
import com.bibintomj.echoscholar.data.repository.ChatRepository
import com.bibintomj.echoscholar.util.getOrCreateLocalUserId
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {

    // TODO: point this to your Next.js/Backend host (no trailing slash).
    // For example: BuildConfig.API_BASE_URL
    private val baseUrl = BuildConfig.API_BASE_URL

    // If you need Supabase bearer token, inject here:
    private val tokenProvider: () -> String? = { null } // or your Supabase token if required

    private val viewModel: ChatViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = ChatRepository(BuildConfig.API_BASE_URL, tokenProvider)
                val userIdProvider = { getOrCreateLocalUserId(this@ChatActivity) } // or supabaseClient.auth.currentUserOrNull()?.id!!
                return ChatViewModel(repo, userIdProvider) as T
            }
        }
    }

    private lateinit var recycler: RecyclerView
    private lateinit var input: EditText
    private lateinit var send: ImageButton
    private lateinit var progress: ProgressBar
    private lateinit var errorText: TextView
    private val adapter = MessageAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recycler = findViewById(R.id.recycler)
        input = findViewById(R.id.inputEditText)
        send = findViewById(R.id.sendButton)
        progress = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        send.setOnClickListener { sendMessage() }

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else false
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collectLatest { list ->
                        adapter.submitList(list) {
                            val last = adapter.itemCount - 1
                            if (last >= 0) recycler.scrollToPosition(last)
                        }
                    }
                }
                launch {
                    viewModel.loading.collectLatest { show ->
                        progress.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }
                launch {
                    viewModel.error.collectLatest { err ->
                        errorText.text = err ?: ""
                        errorText.visibility = if (err.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
                    }
                }
            }
        }
    }

    private fun sendMessage() {
        val text = input.text?.toString()?.trim().orEmpty()
        if (text.isBlank()) return
        viewModel.send(text)
        input.setText("")
    }
}
