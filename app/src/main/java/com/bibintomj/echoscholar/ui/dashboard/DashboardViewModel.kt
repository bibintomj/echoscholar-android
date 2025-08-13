package com.bibintomj.echoscholar.ui.dashboard

import androidx.lifecycle.*
import com.bibintomj.echoscholar.data.model.SessionAPIModel
import com.bibintomj.echoscholar.data.repository.SupabaseRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _sessions = MutableLiveData<List<SessionAPIModel>>()
    val sessions: LiveData<List<SessionAPIModel>> = _sessions

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadSessions() {
        viewModelScope.launch {
            val result = SupabaseRepository.fetchSessions()
            result.onSuccess { _sessions.value = it }
                .onFailure { _error.value = it.message }
        }
    }
    fun deleteSession(id: String) {
        viewModelScope.launch {
            try {
                val token = com.bibintomj.echoscholar.SupabaseManager.supabase.auth
                    .currentSessionOrNull()?.accessToken
                com.bibintomj.echoscholar.di.ServiceLocator.sessionRepository
                    .deleteSession(id, token)
                // Reload list after delete
                loadSessions()
            } catch (t: Throwable) {
                // If you have _error LiveData, set it; otherwise log/toast in Activity
                _error.postValue("Failed to delete: ${t.message}")
            }
        }
    }
}
