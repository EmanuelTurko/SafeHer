package com.example.safeher.model.api

data class NotificationItem(
    val type: String,
    val postId: String,
    val fromUser: FromUser,
    val createdAt: String,
    val read: Boolean
)

data class FromUser(
    val id: String,
    val fullName: String,
    val profilePicture: String?
)

