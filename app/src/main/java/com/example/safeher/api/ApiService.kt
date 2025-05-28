package com.example.safeher.api

import com.example.safeher.model.Comment
import com.example.safeher.model.ContactItem
import com.example.safeher.model.Post
import com.example.safeher.model.RegisterRequest
import com.example.safeher.model.LoginRequest
import com.example.safeher.model.UpdateSafeCircleRequest
import com.example.safeher.model.Test
import com.example.safeher.model.User
import com.example.safeher.model.api.ApiResponse
import com.example.safeher.model.api.CommentRequest
import com.example.safeher.model.api.CreatePostRequest
import com.example.safeher.model.api.CreatePostResponse
import com.example.safeher.model.api.GenericResponse
import com.example.safeher.model.api.TwilioEmergencyMessageRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("test")
    fun getTest(): Call<Test>

    @POST("auth/register")
    suspend fun registerUser(@Body data: RegisterRequest): ApiResponse<Unit>

    @POST("auth/login")
    suspend fun loginUser(@Body data: LoginRequest): ApiResponse<User>

    @POST("auth/updateUserSafeCircle")
    suspend fun updateUserSafeCircle(
        @Body request: UpdateSafeCircleRequest
    ): ApiResponse<Unit>

    @GET("user/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): ApiResponse<User>

    @PUT("user/update-profile/{userId}")
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        @Body user: User
    ): ApiResponse<Unit>

    @POST("post/")
    suspend fun createPost(@Body request: CreatePostRequest
    ): Response<CreatePostResponse>

    @GET("post/")
    suspend fun getAllPosts(): List<Post>

    @PUT("post/{postId}")
    suspend fun editPost(
        @Path("postId") postId: String,
        @Body request: CreatePostRequest
    ): Response<GenericResponse>

    @DELETE("post/{postId}")
    suspend fun deletePost(
        @Path("postId") postId: String
    ): Response<Void>

    @POST("post/{postId}/comment")
    suspend fun createComment(
        @Path("postId") postId: String,
        @Body request: CommentRequest
    ): ApiResponse<Comment>

    @DELETE("comment/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: String
    ): Response<Void>

    @DELETE("users/{id}")
    suspend fun deleteUserAccount(
        @Path("id") userId: String
    ): ApiResponse<Unit>

    @POST("twilio/send-emergency-message")
    suspend fun sendEmergencyMessage(@Body request: TwilioEmergencyMessageRequest): ApiResponse<Unit>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body email: Map<String, String>): ApiResponse<Unit>

}


