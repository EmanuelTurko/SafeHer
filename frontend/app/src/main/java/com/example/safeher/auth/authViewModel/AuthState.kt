package com.example.safeher.auth.authViewModel

import com.google.firebase.auth.FirebaseUser

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val user: FirebaseUser) : AuthState
    data object ForgotPasswordSuccess : AuthState
    data class ForgotPasswordError(val error: String) : AuthState

    data class Error(val message: String) : AuthState
}