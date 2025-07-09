package com.example.safeher

import android.app.Application
import android.content.Context

class SafeHerApp : Application() {
    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
