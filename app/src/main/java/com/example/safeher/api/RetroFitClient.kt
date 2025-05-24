package com.example.safeher.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroFitClient {
    private const val BASE_URL = "http://10.0.2.2:3001/api/"
    private const val LOCAL_URL = "http://192.168.0.121:3001/api/"

    fun getApiService(context: Context): ApiService {
        // Reads the saved token from "auth" prefs
        val tokenProvider: () -> String? = {
            context
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("token", null)
        }

        // Attach Authorization header to every request
        val client = OkHttpClient.Builder()
            .addInterceptor(object : Interceptor {
                override fun intercept(chain: Interceptor.Chain): Response {
                    val token = tokenProvider()
                    val reqBuilder = chain.request().newBuilder()
                    if (!token.isNullOrEmpty()) {
                        reqBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    return chain.proceed(reqBuilder.build())
                }
            })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(LOCAL_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
