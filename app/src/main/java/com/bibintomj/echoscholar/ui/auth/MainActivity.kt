package com.bibintomj.echoscholar.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bibintomj.echoscholar.BuildConfig
import com.bibintomj.echoscholar.databinding.ActivityMainBinding
import com.bibintomj.echoscholar.ui.dashboard.DashboardActivity
import com.bibintomj.echoscholar.SupabaseManager
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var oneTapClient: SignInClient

    private val authViewModel by lazy {
        AuthViewModel(SupabaseManager.supabase.auth)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val credentials = Identity.getSignInClient(this)
                .getSignInCredentialFromIntent(result.data)
            val idToken = credentials.googleIdToken

            Log.d("GoogleAuth", "idToken = $idToken")
            Log.d("GoogleAuth", "clientId = ${BuildConfig.GOOGLE_ANDROID_CLIENT_ID}")

            if (idToken != null) {
                authViewModel.loginWithGoogleIdToken(idToken)
            } else {
                Toast.makeText(this, "Google ID token is null", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        oneTapClient = Identity.getSignInClient(this)
        setupTabSwitching()
        setupUI()
        observeLoginState()
    }

    private fun setupTabSwitching() {
        binding.tabLogin.setOnClickListener {
            binding.loginForm.visibility = View.VISIBLE
            binding.registerForm.visibility = View.GONE
            binding.tabLogin.setBackgroundResource(com.bibintomj.echoscholar.R.drawable.glass_effect_background)
            binding.tabRegister.setBackgroundColor(resources.getColor(android.R.color.transparent))
        }

        binding.tabRegister.setOnClickListener {
            binding.loginForm.visibility = View.GONE
            binding.registerForm.visibility = View.VISIBLE
            binding.tabRegister.setBackgroundResource(com.bibintomj.echoscholar.R.drawable.glass_effect_background)
            binding.tabLogin.setBackgroundColor(resources.getColor(android.R.color.transparent))
        }
    }

    private fun setupUI() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.loginWithEmail(email, password)
            } else {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailRegisterInput.text.toString().trim()
            val password = binding.passwordRegisterInput.text.toString().trim()
            val repeatPassword = binding.repeatpasswordInput.text.toString().trim()

            if (email.isNotEmpty() && password == repeatPassword) {
                authViewModel.registerWithEmail(email, password)
            } else {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }

        binding.googleButton.setOnClickListener {
            val request = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(BuildConfig.GOOGLE_ANDROID_CLIENT_ID)
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                )
                .build()


            oneTapClient.beginSignIn(request)
                .addOnSuccessListener { result ->
                    googleSignInLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Google Sign-In failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            authViewModel.loginState.collectLatest { result ->
                result?.onSuccess {
                    startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                    finish()
                }?.onFailure {
                    Toast.makeText(this@MainActivity, "Authentication failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
