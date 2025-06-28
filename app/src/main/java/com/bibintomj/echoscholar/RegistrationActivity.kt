package com.bibintomj.echoscholar

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bibintomj.echoscholar.databinding.ActivityRegistrationBinding
import com.bibintomj.echoscholar.repository.AuthRepository
import kotlinx.coroutines.launch

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener {
            val email = binding.emailRegisterInput.text.toString().trim()
            val password = binding.passwordRegisterInput.text.toString().trim()
            val repeatPassword = binding.repeatpasswordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != repeatPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = authRepository.register(email, password)
                if (result.isSuccess) {
                    Toast.makeText(this@RegistrationActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@RegistrationActivity, "Registration failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.tabLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
