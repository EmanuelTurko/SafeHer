package com.example.safeher.auth.authViewModel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.auth.authRepository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.signUp(email, password)
                .onSuccess { user ->
                    user?.let {
                        _authState.value = AuthState.Success(it)
                    } ?: run {
                        _authState.value = AuthState.Error("Sign in failed")
                    }
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.signIn(email, password)
                .onSuccess { user ->
                    user?.let {
                        _authState.value = AuthState.Success(user)
                    } ?: run {
                        _authState.value = AuthState.Error("Sign up failed")
                    }
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.forgotPassword(email)
                .onSuccess { bool ->
                    _authState.value = AuthState.ForgotPasswordSuccess
                }
                .onFailure { exception ->
                    _authState.value = AuthState.ForgotPasswordError(exception.message ?: "Sign in failed")
                }
        }
    }


    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Idle
    }

    fun getCurrentUser() : Boolean {
        val user = repository.getCurrentUser()
        return user != null
    }

    fun validateInput(email: String, password: String): Pair<Boolean, String> {
        var isValid = true
        var errorText = ""

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText = "Invalid email address"
            isValid = false
        }

        if (password.isEmpty() || password.length < 6) {
            errorText = "Password must be at least 6 characters"
            isValid = false
        }

        return Pair(isValid, errorText)
    }

    fun forgotPasswordValidateInput(email: String): Pair<Boolean, String> {
        var isValid = true
        var errorText = ""

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorText = "Invalid email address"
            isValid = false
        }

        return Pair(isValid, errorText)
    }
}