package com.example.safeher.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val user: User,
    val body: String,
    val createdAt: String
)
