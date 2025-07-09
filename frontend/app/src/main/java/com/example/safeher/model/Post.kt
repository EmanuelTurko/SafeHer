package com.example.safeher.model
import com.google.gson.annotations.SerializedName


data class Post(
    @SerializedName("_id") val id: String,
    @SerializedName("body") val body: String,
    @SerializedName("comments") val comments: MutableList<Comment> = mutableListOf(),
    @SerializedName("user") val user: User,
    @SerializedName("likes") val likes: List<String>,
    @SerializedName("likeCount") var likeCount: Int,
    @SerializedName("commentCount") var commentCount: Int,
    @SerializedName("isLiked") var isLiked: Boolean,
    @SerializedName("createdAt") val createdAt: String,
)



