package com.bibintomj.echoscholar

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import io.github.jan.supabase.auth.auth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val appName = findViewById<ImageView>(R.id.appName)

        // Load animations
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_fade_scale)
        val textAnim = AnimationUtils.loadAnimation(this, R.anim.text_fade_in)

        // Start animations
        logoImage.startAnimation(logoAnim)
        appName.startAnimation(textAnim)

        // Navigate to next screen after animations
        appName.postDelayed({
            val session = SupabaseManager.supabase.auth.currentSessionOrNull()
            val nextScreen = if (session != null) {
                DashboardActivity::class.java
            } else {
                MainActivity::class.java
            }
            startActivity(Intent(this, nextScreen))
            finish()
        }, 2200) // Delay for animation completion
    }
}
