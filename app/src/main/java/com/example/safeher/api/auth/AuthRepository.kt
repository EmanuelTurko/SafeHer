package com.example.safeher.api.auth

import com.example.safeher.api.ApiService
import com.example.safeher.model.ContactItem
import com.example.safeher.model.api.ApiResponse
import com.example.safeher.model.LoginRequest
import com.example.safeher.model.RegisterRequest
import com.example.safeher.model.User

class AuthRepository(private val apiService: ApiService) {
    suspend fun registerUser(data: RegisterRequest): ApiResponse<Unit>{
        return apiService.registerUser(data)
    }
    suspend fun loginUser(data: LoginRequest): ApiResponse<User>{
        return apiService.loginUser(data)
    }
}