package com.example.safeher.api

import com.example.safeher.model.ContactItem
import com.example.safeher.model.api.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import com.example.safeher.model.Test
import com.example.safeher.model.User
import com.example.safeher.model.RegisterRequest
import com.example.safeher.model.LoginRequest
import com.example.safeher.model.UpdateSafeCircleRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @GET("/test")
    fun getTest(): Call<Test>


    @POST("auth/register")
    suspend fun registerUser(@Body data: RegisterRequest): ApiResponse<Unit>

    @POST("auth/login")
    suspend fun loginUser(@Body data: LoginRequest): ApiResponse<User>

    @POST("auth/updateUserSafeCircle")
    suspend fun updateUserSafeCircle(@Body data: UpdateSafeCircleRequest): ApiResponse<ContactItem>


}