package com.example.safeher.model
import com.google.gson.annotations.SerializedName


data class Post(
    @SerializedName("_id") val id: String,
    @SerializedName("body") val body: String,
    @SerializedName("comments") val comments: MutableList<Comment> = mutableListOf(),
    @SerializedName("user") val user: User,
    var likeCount: Int,
    val commentCount: Int,
    @SerializedName("createdAt") val createdAt: String,
//    val likes: List<String> = emptyList(),
)



