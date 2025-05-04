package com.example.safeher.auth.pair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.safeher.api.ApiService

class PairViewModelFactory(private val apiService: ApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PairViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PairViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}