package com.example.safeher.api

import retrofit2.Call
import retrofit2.http.GET
import com.example.safeher.model.Test
interface ApiService {
    @GET("/test")
    fun getTest(): Call<Test>
}