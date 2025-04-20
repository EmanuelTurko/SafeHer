package com.example.safeher.api.bluetooth

import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.ApiResponse
import retrofit2.Call

class BluetoothRepository {
    fun sendCommand(command:String): Call<ApiResponse> {
        return RetroFitClient.apiService.sendCommand(CommandRequest(command))
    }
}