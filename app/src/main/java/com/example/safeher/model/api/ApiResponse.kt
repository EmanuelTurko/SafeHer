package com.example.safeher.model.api

data class ApiResponse<T>(
    val message: String? = null,
    val error: String? = null,
    val data: T? = null
)