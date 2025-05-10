package com.example.safeher.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroFitClient {
    private const val BASE_URL = "http://10.0.2.2:3001/api/"
    private const val LOCAL_URL = "http://192.168.0.121:3001/api/"

    fun getApiService(context:Context): ApiService{
        val tokenProvider = {
            val tokenPrefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            tokenPrefs.getString("token", null)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }


    /*val apiService: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(LOCAL_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }*/
}