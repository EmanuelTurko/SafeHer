package com.example.safeher.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.fragment.NavHostFragment
import com.example.safeher.R
import com.example.safeher.home_screen.HomeScreenActivity
import com.example.safeher.utils.hideSystemUI
import com.example.safeher.utils.setupUI

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val token = prefs.getString("token", null)

        if (!token.isNullOrEmpty()) {
            // User is already logged in, go to HomePageActivity
            startActivity(Intent(this, HomeScreenActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        // Obtain reference to the NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // Get the NavController
        navController = navHostFragment.navController
        setupUI(findViewById(android.R.id.content))
        hideSystemUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}