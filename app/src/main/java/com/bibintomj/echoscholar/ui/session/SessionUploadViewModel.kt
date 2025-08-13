package com.bibintomj.echoscholar.ui.session

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bibintomj.echoscholar.data.model.SaveSessionRequest
import com.bibintomj.echoscholar.data.model.SaveSessionResponse
import com.bibintomj.echoscholar.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class UploadState {
    data object Idle : UploadState()
    data object Loading : UploadState()
    data class Success(val response: SaveSessionResponse) : UploadState()
    data class Error(val message: String) : UploadState()
}

class SessionUploadViewModel(
    private val repository: SessionRepository,
    private val saveSessionUrl: String,      // e.g. BuildConfig.API_BASE_URL + "/api/session/save"
    private val getAuthToken: suspend () -> String? // inject a lambda that fetches your Supabase token
) : ViewModel() {

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state

    /**
     * Reads all bytes from a content Uri. Prefer using a file/uri to avoid keeping huge audio fully in RAM.
     */
    private fun readBytes(contentResolver: ContentResolver, uri: Uri): ByteArray {
        contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("Unable to open InputStream for $uri")
            return input.readBytes()
        }
    }

    fun saveSessionFromUri(
        contentResolver: ContentResolver,
        audioUri: Uri,
        transcription: String,
        translation: String,
        targetLanguage: String,
        audioFileName: String = "audio.wav",
        mimeType: String = "audio/wav"
    ) {
        viewModelScope.launch {
            _state.value = UploadState.Loading
            try {
                val bytes = readBytes(contentResolver, audioUri)
                Log.d("SessionUploadVM", "Preparing upload: bytes=${bytes.size}, url=$saveSessionUrl")
                if (bytes.isEmpty()) {
                    _state.value = UploadState.Error("Empty audio file – nothing to upload")
                    return@launch
                }
                val req = SaveSessionRequest(
                    transcription = transcription,
                    translation = translation,
                    targetLanguage = targetLanguage,
                    audioBytes = bytes,
                    audioFileName = audioFileName,
                    mimeType = mimeType
                )
                val token = getAuthToken()
                Log.d("SessionUploadVM", "Auth token present? ${!token.isNullOrBlank()}")

                val res = repository.saveSession(
                    saveSessionUrl = saveSessionUrl,
                    request = req,
                    authToken = token
                )
                _state.value = UploadState.Success(res)

            } catch (t: Throwable) {
                Log.e("SessionUploadVM", "Upload failed", t)
                _state.value = UploadState.Error(t.message ?: "Failed to save session")
            }



        }
    }
}
