package com.example.safeher.api

import android.content.Context
import com.example.safeher.SafeHerApp
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroFitClient {
    private const val BASE_URL = "http://10.0.2.2:3001/api/"
    private const val LOCAL_URL = "http://192.168.0.121:3001/api/"

    private fun provideClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = SafeHerApp.appContext
                    .getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
                    .getString("AUTH_TOKEN", null)
                val reqBuilder = chain.request().newBuilder()
                token?.let { reqBuilder.addHeader("Authorization", "Bearer $it") }
                chain.proceed(reqBuilder.build())
            }
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
