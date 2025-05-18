package com.example.safeher.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id") val id: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("profilePicture") val profilePicture: String?,
    val email: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val idPhotoUrl: String = "",
    val accessToken: String? = null,
    val safeCircleContacts: List<ContactItem> = emptyList()
)