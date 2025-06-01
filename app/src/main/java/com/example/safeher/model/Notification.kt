package com.example.safeher.model

data class Notification(
    val type: String,
    val postId: String,
    val createdAt: String,
    val fromUser: FromUser
)

data class FromUser(
    val id: String,
    val fullName: String,
    val profilePicture: String? = null
)
