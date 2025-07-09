package com.example.safeher.utils

import android.os.Build

object SdkVersion {
    val VersionCodeS:Boolean  = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // sdk 31 or higher
    val VersionCodeTiramisu:Boolean  = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) // sdk 33 or higher
    val scanTimeoutMs = 15000L // 15 seconds
}