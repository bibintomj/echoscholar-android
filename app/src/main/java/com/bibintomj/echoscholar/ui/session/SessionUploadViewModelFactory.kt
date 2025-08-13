package com.bibintomj.echoscholar.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bibintomj.echoscholar.data.repository.SessionRepository

class SessionUploadViewModelFactory(
    private val repository: SessionRepository,
    private val saveSessionUrl: String,
    private val getAuthToken: suspend () -> String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SessionUploadViewModel::class.java))
        return SessionUploadViewModel(repository, saveSessionUrl, getAuthToken) as T
    }
}
