package com.example.safeher.model.api

data class CreatePostRequest(
    val body: String,
    val isAnonymous: Boolean = false
)

data class CreatePostResponse(
    val message: String,
    val id: String
)
