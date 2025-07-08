package com.bibintomj.echoscholar.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.ExternalAuthConfig
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val auth: Auth) : ViewModel() {

    private val _loginState = MutableStateFlow<Result<UserSession>?>(null)
    val loginState: StateFlow<Result<UserSession>?> = _loginState

    // Public wrappers to call from UI
    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch { loginWithEmailInternal(email, password) }
    }

    fun registerWithEmail(email: String, password: String) {
        viewModelScope.launch { registerWithEmailInternal(email, password) }
    }

    fun loginWithGoogleIdToken(idToken: String) {
        viewModelScope.launch { loginWithGoogleOAuthInternal() }
    }

    // Internal logic encapsulated privately
    private suspend fun loginWithEmailInternal(email: String, password: String) {
        try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val session = auth.currentSessionOrNull()
            _loginState.value = session?.let { Result.success(it) }
                ?: Result.failure(Exception("Login succeeded but session is null"))
        } catch (e: Exception) {
            _loginState.value = Result.failure(e)
        }
    }

    private suspend fun registerWithEmailInternal(email: String, password: String) {
        try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val session = auth.currentSessionOrNull()
            _loginState.value = session?.let { Result.success(it) }
                ?: Result.failure(Exception("Registration succeeded but session is null"))
        } catch (e: Exception) {
            _loginState.value = Result.failure(e)
        }
    }

    // ✅ Private method for OAuth
    private suspend fun loginWithGoogleOAuthInternal() {
        try {
            auth.signInWith<ExternalAuthConfig, Unit, Google>(provider = Google)

            val session = auth.currentSessionOrNull()
            val user = auth.currentUserOrNull()

            Log.d("SUPABASE", "Metadata: ${user?.userMetadata}")

            // ✅ Send name and avatar if available
            val name = user?.userMetadata?.get("name") as? String
            val avatarUrl = user?.userMetadata?.get("avatar_url") as? String

        } catch (e: Exception) {
            Log.e("OAuthLoginError", "Failed to login with Google OAuth", e)
            _loginState.value = Result.failure(e)
        }
    }

}
