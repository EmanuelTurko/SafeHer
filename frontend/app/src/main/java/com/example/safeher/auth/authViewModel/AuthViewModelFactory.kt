package com.example.safeher.auth.authViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.safeher.api.auth.AuthRepository

class AuthViewModelFactory(private val authRepository: AuthRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(AuthViewModelApi::class.java)) {
            return AuthViewModelApi(authRepository) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}