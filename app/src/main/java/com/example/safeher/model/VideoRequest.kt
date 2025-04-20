package com.example.safeher.model

data class VideoRequest(
    val frameRate: Int = 12,
    val framesData: List<String>
)
