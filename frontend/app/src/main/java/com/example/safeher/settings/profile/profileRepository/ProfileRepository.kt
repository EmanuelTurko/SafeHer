// ProfileRepository.kt
package com.example.safeher.settings.profile.profileRepository

import android.content.Context
import android.util.Log
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.User

class ProfileRepository(context: Context) {
    private val api = RetroFitClient.getApiService(context)

    suspend fun getUserData(userId: String): Result<User?> = runCatching {
        Log.d("ProfileRepo", "Fetching profile for userId='$userId'")
        val resp = api.getUserProfile(userId)
        Log.d("ProfileRepo", "Response.error=${resp.error}, data=${resp.data}")
        if (resp.error == null) resp.data
        else throw Exception(resp.error)
    }

    suspend fun saveUserData(userId: String, user: User): Result<Unit> = runCatching {
        val resp = api.updateUserProfile(userId, user)
        if (resp.error == null) Unit
        else throw Exception(resp.error)
    }

    suspend fun deleteAccount(userId: String): Result<Unit> = runCatching {
        Log.d("ProfileRepository", "Sending DELETE request for userId=$userId")
        val resp = api.deleteUser(userId)
        Log.d("ProfileRepository", "Response code: ${resp.code()}, success: ${resp.isSuccessful}")
        if (resp.isSuccessful) Unit
        else throw Exception("Failed to delete user: ${resp.code()}")
    }




    fun signOut() = Unit
}
