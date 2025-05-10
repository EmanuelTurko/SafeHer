package com.example.safeher.auth.safeCircle

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.api.ApiService
import com.example.safeher.model.ContactItem
import com.example.safeher.model.UpdateSafeCircleRequest
import com.example.safeher.model.api.ApiResponse
import kotlinx.coroutines.launch
import android.util.Log

class SafeCircleViewModel(private val apiService: ApiService): ViewModel() {
    private val _updateSafeCircleResponse = MutableLiveData<ApiResponse<ContactItem>>()

    fun updateUserSafeCircle(fullName:String, safeCircle: List<String>){
        viewModelScope.launch{
            try{
                val request = UpdateSafeCircleRequest(fullName,safeCircle)
                Log.d("PairFragment", "Request to update safe circle: $request")
                val response = apiService.updateUserSafeCircle(request)
                Log.d("PairFragment", "Got update response: $response")
                _updateSafeCircleResponse.postValue(response)
            } catch(e: Exception){
                Log.e("PairFragment", "Error updating safe circle: ${e.message}")
                _updateSafeCircleResponse.postValue(ApiResponse(error = e.message))
            }
        }
    }


}