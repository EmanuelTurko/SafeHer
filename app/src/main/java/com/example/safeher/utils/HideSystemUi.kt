package com.example.safeher.utils

import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
fun ComponentActivity.hideSystemUI() {
    val windowInsetsController = window.insetsController
    windowInsetsController?.hide(WindowInsets.Type.navigationBars())
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
}