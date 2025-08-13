package com.bibintomj.echoscholar.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibintomj.echoscholar.data.model.ChatMessage
import com.bibintomj.echoscholar.data.model.ChatRequest
import com.bibintomj.echoscholar.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: ChatRepository,
    // Provide a userId (Supabase user id or locally persisted UUID)
    private val userIdProvider: () -> String
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun send(text: String) {
        if (text.isBlank()) return
        if (_loading.value) return // avoid double send

        // user's bubble
        _messages.update { it + ChatMessage(text = text, isUser = true) }
        // placeholder assistant bubble
        _messages.update { it + ChatMessage(text = "", isUser = false) }

        _loading.value = true
        _error.value = null

        viewModelScope.launch {
            val builderIndex = _messages.value.lastIndex
            val request = ChatRequest(
                question = text,
                userId = userIdProvider()
            )

            val built = StringBuilder()
            try {
                repo.streamChat(request).collect { token ->
                    when {
                        token.startsWith("[error]") -> _error.value = token.removePrefix("[error]").trim()
                        token == "[done]" || token.startsWith("[debug]") -> Unit // ignore in UI
                        else -> {
                            built.append(token)
                            _messages.update { current ->
                                if (builderIndex in current.indices) {
                                    current.toMutableList().also { list ->
                                        list[builderIndex] = list[builderIndex].copy(text = built.toString())
                                    }
                                } else current
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                _error.value = t.message ?: "Stream failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearChat() { _messages.value = emptyList() }
}
