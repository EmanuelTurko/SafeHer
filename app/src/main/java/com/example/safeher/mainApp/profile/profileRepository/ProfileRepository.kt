package com.example.safeher.mainApp.profile.profileRepository

import android.util.Log
import com.example.safeher.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val userReference = database.getReference("users")
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: throw Exception("User not authenticated")

    suspend fun getUserData(): Result<User?> =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val snapshot = userReference.child(userId).get().await()
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    user
                } else {
                    null
                }
            }
    }

    suspend fun saveUserData(user: User): Result<Boolean> =
        withContext(Dispatchers.IO){
            kotlin.runCatching {
                userReference.child(userId).setValue(user)
                true
            }
        }

    suspend fun updateUserEmail(newEmail: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                auth.currentUser?.updateEmail(newEmail)
                true
            }
        }

    suspend fun updateUserPassword(password: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                auth.currentUser?.updatePassword(password)
                true
            }
        }

    fun signOut() {
        auth.signOut()
    }

}