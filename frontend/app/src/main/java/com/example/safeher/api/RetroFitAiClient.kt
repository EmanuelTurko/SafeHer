package com.example.safeher.api

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroFitAiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val API_KEY = "AIzaSyAyuktmQydXXfmfvfAoK5tsGKR8YHEwMMU"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val originalUrl: HttpUrl = chain.request().url
            val newUrl = originalUrl.newBuilder()
                .addQueryParameter("key", API_KEY)
                .build()


            val request:Request = chain.request().newBuilder()
                .url(newUrl)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    val api: GeminiApiServices by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        retrofit.create(GeminiApiServices::class.java)
    }
}