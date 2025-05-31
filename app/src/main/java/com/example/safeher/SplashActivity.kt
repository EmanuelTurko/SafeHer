package com.example.safeher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_SafeHer_Splash)
        super.onCreate(savedInstanceState)

        startActivity(Intent(this, com.example.safeher.auth.MainActivity::class.java))
        finish()
    }
}
