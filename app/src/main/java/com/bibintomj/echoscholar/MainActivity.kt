package com.bibintomj.echoscholar

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bibintomj.echoscholar.databinding.ActivityMainBinding
import com.bibintomj.echoscholar.repository.AuthRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tabLogin = findViewById<TextView>(R.id.tabLogin)
        val tabRegister = findViewById<TextView>(R.id.tabRegister)
        val loginForm = findViewById<LinearLayout>(R.id.loginForm)
        val registerForm = findViewById<LinearLayout>(R.id.registerForm)

        // Tab switching
        tabLogin.setOnClickListener {
            loginForm.visibility = View.VISIBLE
            registerForm.visibility = View.GONE
            tabLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.accent))
            tabRegister.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive))
        }

        tabRegister.setOnClickListener {
            loginForm.visibility = View.GONE
            registerForm.visibility = View.VISIBLE
            tabRegister.setBackgroundColor(ContextCompat.getColor(this, R.color.accent))
            tabLogin.setBackgroundColor(ContextCompat.getColor(this, R.color.tab_inactive))
        }

        // Login button logic
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            lifecycleScope.launch {
                val result = authRepository.login(email, password)
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "Login successful", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to next screen
                } else {
                    Toast.makeText(this@MainActivity, "Login failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Register button logic
        binding.registerButton.setOnClickListener {
            val email = binding.registerEmailInput.text.toString().trim()
            val password = binding.registerPasswordInput.text.toString().trim()

            lifecycleScope.launch {
                val result = authRepository.register(email, password)
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Registration failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
