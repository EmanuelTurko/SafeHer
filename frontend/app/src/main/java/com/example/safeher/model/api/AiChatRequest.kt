package com.example.safeher.model.api

data class AiChatRequest(
    val contents: List<Content>
)
data class Content(
    val parts: List<Part>
)
data class Part(
    val text: String
)