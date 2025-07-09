package com.example.safeher.model

import com.google.gson.annotations.SerializedName

data class Comment(
    @SerializedName("_id") val id: String,
    @SerializedName("user") val user: User,
    @SerializedName("post") val postId: String,
    @SerializedName("body") val body: String,
    @SerializedName("createdAt") val createdAt: String
)
