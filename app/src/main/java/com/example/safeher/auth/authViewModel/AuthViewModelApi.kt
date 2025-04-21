package com.example.safeher.auth.authViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.api.auth.AuthRepository
import com.example.safeher.model.ApiResponse
import com.example.safeher.model.LoginRequest
import com.example.safeher.model.RegisterRequest
import com.example.safeher.model.User
import kotlinx.coroutines.launch

class AuthViewModelApi(private val authRepository: AuthRepository) :ViewModel() {
    val _registerResponse = MutableLiveData<ApiResponse<Unit>>()
    val registerResponse: LiveData<ApiResponse<Unit>> get() = _registerResponse

    val _loginResponse = MutableLiveData<ApiResponse<User>>()
    val loginResponse: LiveData<ApiResponse<User>> get() = _loginResponse

    fun registerUser(data: RegisterRequest){
        viewModelScope.launch {
            try{
                val response = authRepository.registerUser(data)
                _registerResponse.postValue(response)
            } catch( e: Exception){
                _registerResponse.postValue(ApiResponse(error = e.message))
            }
        }
    }
    fun loginUser(data: LoginRequest){
        viewModelScope.launch{
            try{
                val response = authRepository.loginUser(data)
                _loginResponse.postValue(response)
            } catch( e: Exception){
                _loginResponse.postValue(ApiResponse(error = e.message))
            }
        }
    }
}