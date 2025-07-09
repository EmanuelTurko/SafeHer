package com.example.safeher.model.api

data class AiChatResponse(
    val candidates : List<Candidate>
)
data class Candidate(
    val content: Content
)

