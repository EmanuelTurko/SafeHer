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
import com.example.safeher.model.ContactPayload

class SafeCircleViewModel(private val apiService: ApiService): ViewModel() {
    private val _updateSafeCircleResponse = MutableLiveData<ApiResponse<Unit>>()
    val updateSafeCircleResponse = _updateSafeCircleResponse

    fun updateUserSafeCircle(fullName: String, contacts: List<ContactItem>) {
        viewModelScope.launch {
            try {
                Log.d("SafeCircleVM", " Sending request to update safe circle")

                // ממפה רק name ו־phoneNumber – בלי isSelected
                val contactPayloadList = contacts.map {
                    ContactPayload(name = it.name, phoneNumber = it.phoneNumber)
                }

                val request = UpdateSafeCircleRequest(fullName, contactPayloadList)
                Log.d("SafeCircleVM", "Sending request: $request")

                val response = apiService.updateUserSafeCircle(request)
                Log.d("SafeCircleVM", "Response: $response")

                _updateSafeCircleResponse.postValue(response)
            } catch (e: Exception) {
                Log.e("SafeCircleVM", "Error updating safe circle", e)
                _updateSafeCircleResponse.postValue(ApiResponse(error = e.message))
            }
        }
    }




}
