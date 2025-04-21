package com.example.safeher.api

import com.example.safeher.model.api.AiChatRequest
import com.example.safeher.model.api.AiChatResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Response

interface GeminiApiServices {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(@Body request: AiChatRequest): Response<AiChatResponse>
}