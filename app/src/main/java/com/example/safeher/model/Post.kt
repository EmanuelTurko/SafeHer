package com.example.safeher.model

data class Post(
    val id: String = "",
    val image: String? = null,
    val body: String = "",
    val userId: String = "",
    val comments: List<Comment> = emptyList()
)
