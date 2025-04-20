package com.example.safeher.api

import com.example.safeher.api.bluetooth.CommandRequest
import com.example.safeher.model.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import com.example.safeher.model.Test
import com.example.safeher.model.VideoRequest
import com.example.safeher.model.VideoResponse
import com.google.protobuf.Api
import retrofit2.http.Body

data class ConnectRequest(val deviceId: String)
interface ApiService {
    @GET("/test")
    fun getTest(): Call<Test>

    @GET("/start-scanning")
    fun startScanning(): Call<ApiResponse>

    @POST("/connect-device")
    fun connectDevice(@Body request: ConnectRequest): Call<ApiResponse>

    @POST("/send-command") // should send 'START' & 'STOP' commands
    fun sendCommand(@Body request: CommandRequest): Call<ApiResponse>

    @GET("/disconnect")
    fun disconnect(): Call<ApiResponse>

    @POST("/create-video")
    fun createVideo(@Body request: VideoRequest): Call<VideoResponse>


}