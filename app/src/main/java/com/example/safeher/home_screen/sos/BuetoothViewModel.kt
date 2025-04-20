package com.example.safeher.home_screen.sos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.safeher.api.bluetooth.BluetoothRepository
import com.example.safeher.model.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class BluetoothViewModel: ViewModel() {
    private val repository = BluetoothRepository()

    private val _sosResponse = MutableLiveData<ApiResponse>()
    val sosResponse: LiveData<ApiResponse> get() = _sosResponse

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun sendCommand(command: String) {
        repository.sendCommand(command).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    _sosResponse.postValue(response.body())
                } else {
                    _error.postValue("Failed to send command: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                _error.postValue(t.message ?: "Unknown error")
            }
        })
    }
}