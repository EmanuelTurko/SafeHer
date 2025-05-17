package com.example.safeher.model.api

data class CreatePostRequest(
    val body: String
)

data class CreatePostResponse(
    val message: String,
    val id: String
)
