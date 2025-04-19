package com.example.safeher.settings.profile.profileViewModel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.settings.profile.profileRepository.ProfileRepository
import com.example.safeher.model.User
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()

    private val _profileState = MutableLiveData<ProfileState>()
    val profileState: LiveData<ProfileState> = _profileState

    fun getUserData() {
        viewModelScope.launch {
            repository.getUserData()
                .onSuccess { user ->
                    user?.let {
                        _profileState.value = ProfileState.GetUserDataSuccess(user = user)
                    }.run {
                        _profileState.value = ProfileState.GetUserDataError("please try again later")
                    }
                }
                .onFailure { exception ->
                    _profileState.value = ProfileState.GetUserDataError(exception.message ?: "")
                }
        }
    }

    fun saveUserData(user: User) {
        viewModelScope.launch {
            repository.saveUserData(user)
                .onSuccess {
                    _profileState.value = ProfileState.SaveUserDataSuccess
                }
                .onFailure { exception ->
                    _profileState.value = ProfileState.GetUserDataError(exception.message ?: "Sign in failed")
                }
        }
    }


     fun updateUserEmail(newEmail: String) {
         viewModelScope.launch {
             repository.updateUserEmail(newEmail)
                 .onSuccess {
                     _profileState.value = ProfileState.UpdateEmailSuccess
                 }
                 .onFailure { exception ->
                     _profileState.value = ProfileState.GetUserDataError(exception.message ?: "Update Email Error")
                 }
         }
    }

    fun updateUserPassword(newPassword: String) {
        viewModelScope.launch {
            repository.updateUserPassword(newPassword)
                .onSuccess {
                    _profileState.value = ProfileState.UpdatePasswordSuccess
                }
                .onFailure { exception ->
                    _profileState.value = ProfileState.UpdatePasswordError(exception.message ?: "Update Password Error")
                }
        }
    }


    fun convertImageToBase64(inputStream: InputStream) : String? {
        try {
            //val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Convert the bitmap to a byte array
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // Convert byte array to Base64 string
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            return base64String
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun convertBase64ToBitmap(base64String: String): Bitmap? {
        try {
            // Decode the Base64 string
            val decodedString: ByteArray = Base64.decode(base64String, Base64.DEFAULT)

            // Convert the decoded byte array into a Bitmap
            val decodedByte: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

            // Set the Bitmap to the ImageView
            return  decodedByte
        } catch (e: Exception) {
            e.printStackTrace() // Handle any exceptions that occur
            return null
        }
    }

    fun convertBitmapToBase64(bitmap: Bitmap): String {
        // Create a ByteArrayOutputStream to hold the bytes of the Bitmap
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Compress the Bitmap into the output stream as PNG or JPEG (choose format as needed)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        // Convert the byte output stream to a byte array
        val byteArray = byteArrayOutputStream.toByteArray()

        // Encode the byte array to a Base64 string
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }


    fun signOut() {
        repository.signOut()
//        _profileState.value = ProfileState.Idle
    }

}