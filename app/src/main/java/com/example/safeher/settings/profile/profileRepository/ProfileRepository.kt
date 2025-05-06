package com.example.safeher.settings.profile.profileRepository

import com.example.safeher.api.RetroFitClient
import com.example.safeher.api.ApiService
import com.example.safeher.model.User

class ProfileRepository {
    private val api = RetroFitClient.apiService

    suspend fun getUserData(userId: String): Result<User?> = runCatching {
        val resp = api.getUserProfile(userId)
        if (resp.error == null) resp.data else throw Exception(resp.error)
    }

    suspend fun saveUserData(userId: String, user: User): Result<Unit> = runCatching {
        val resp = api.updateUserProfile(userId, user)
        if (resp.error == null) Unit else throw Exception(resp.error)
    }

    fun signOut() = Unit
}

