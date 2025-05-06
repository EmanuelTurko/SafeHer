package com.example.safeher.model

data class Post(
    val id: String = "",
    val imageUrl: String? = null,
    val text: String = "",
    val userId: String = "",
    val comments: List<Comment> = emptyList()
)
