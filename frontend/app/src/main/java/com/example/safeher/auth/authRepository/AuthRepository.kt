package com.example.safeher.auth.authRepository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun signUp(email: String, password: String): Result<FirebaseUser?> =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user
            }
        }


    suspend fun signIn(email: String, password: String): Result<FirebaseUser?> =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user
            }
        }

    suspend fun forgotPassword(email: String): Result<Boolean?> =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                auth.sendPasswordResetEmail(email).await()
                true
            }
        }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
}