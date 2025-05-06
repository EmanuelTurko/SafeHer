package com.example.safeher.api

import com.example.safeher.model.ContactItem
import com.example.safeher.model.api.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import com.example.safeher.model.Test
import com.example.safeher.model.User
import com.example.safeher.model.RegisterRequest
import com.example.safeher.model.LoginRequest
import com.example.safeher.model.Post
import com.example.safeher.model.UpdateSafeCircleRequest
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("/test")
    fun getTest(): Call<Test>


    @POST("auth/register")
    suspend fun registerUser(@Body data: RegisterRequest): ApiResponse<Unit>

    @POST("auth/login")
    suspend fun loginUser(@Body data: LoginRequest): ApiResponse<User>

    @POST("auth/updateUserSafeCircle")
    suspend fun updateUserSafeCircle(@Body data: UpdateSafeCircleRequest): ApiResponse<ContactItem>
    @GET("user/{userId}")
    suspend fun getUserProfile(
        @Path("userId") userId: String
    ): ApiResponse<User>

    @PUT("user/update-profile/{userId}")
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        @Body user: User
    ): ApiResponse<Unit>

    @GET("post/")
    suspend fun getAllPosts(): List<Post>

    @POST("post/")
    suspend fun createPost(@Body post: Post)
}