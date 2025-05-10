package com.example.safeher.model

data class User(
    val id: String? = null,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val idPhotoUrl: String = "",
    val profilePicture: String = "",
    val accessToken: String? = null,
    val safeCircleContacts: List<String> = emptyList<String>()
)