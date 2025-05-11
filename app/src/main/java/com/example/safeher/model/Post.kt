package com.example.safeher.model
import com.google.gson.annotations.SerializedName


data class Post(
    @SerializedName("_id") val id: String,
    @SerializedName("body") val body: String,
    @SerializedName("comments") val comments: List<Comment> = emptyList()
//    val user: User,
//    val likes: List<String> = emptyList(),
)



