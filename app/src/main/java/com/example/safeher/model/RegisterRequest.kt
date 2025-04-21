package com.example.safeher.model

data class RegisterRequest(
    val fullName : String = "",
    val email: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val idPhotoUrl: String = "",
)