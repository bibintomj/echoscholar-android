package com.bibintomj.echoscholar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bibintomj.echoscholar.databinding.ActivityMainBinding
import com.bibintomj.echoscholar.repository.AuthRepository
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Email/password login
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = authRepository.login(email, password)
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "Login successful", Toast.LENGTH_SHORT).show()
                    goToDashboard()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Login failed: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Google Sign-In setup
        Log.d("TAG", BuildConfig.GOOGLE_ANDROID_CLIENT_ID)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_ANDROID_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.googleButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLoginLauncher.launch(signInIntent)
        }

        // Navigate to Registration
        binding.tabRegister.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
    }

    private val googleLoginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                lifecycleScope.launch {
                    val result = authRepository.loginWithGoogleIdToken(idToken)
                    if (result.isSuccess) {
                        Toast.makeText(this@MainActivity, "Google login successful!", Toast.LENGTH_SHORT).show()
                        goToDashboard()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Google login failed: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Google ID token is null", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
