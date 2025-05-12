package com.example.safeher.model
import com.google.gson.annotations.SerializedName


data class Post(
    @SerializedName("_id") val id: String,
    @SerializedName("body") val body: String,
    @SerializedName("comments") val comments: List<Comment> = emptyList(),
    @SerializedName("user") val user: User,
    @SerializedName("createdAt") val createdAt: String,
//    val likes: List<String> = emptyList(),
)



