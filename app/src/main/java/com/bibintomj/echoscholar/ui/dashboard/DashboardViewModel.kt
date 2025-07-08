package com.bibintomj.echoscholar.ui.dashboard

import androidx.lifecycle.*
import com.bibintomj.echoscholar.data.model.SessionAPIModel
import com.bibintomj.echoscholar.data.repository.SupabaseRepository
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
}
