package com.example.safeher.settings.profile.profileViewModel

import ProfileState
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.*
import com.example.safeher.model.User
import com.example.safeher.settings.profile.profileRepository.ProfileRepository
import com.example.safeher.settings.profile.profileViewModel.ProfileViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ProfileRepository(application)
    private val _profileState = MutableLiveData<ProfileState>()
    val profileState: LiveData<ProfileState> = _profileState

    fun getUserData(userId: String) {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            repo.getUserData(userId)
                .onSuccess { user ->
                    _profileState.value = ProfileState.GetUserDataSuccess(user)
                }
                .onFailure { ex ->
                    _profileState.value = ProfileState.ProfileError(ex.message ?: "Error loading profile")
                }
        }
    }

    fun saveUserData(user: User) {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            repo.saveUserData(user.id ?: return@launch, user)
                .onSuccess {
                    _profileState.value = ProfileState.SaveUserDataSuccess
                }
                .onFailure { ex ->
                    _profileState.value = ProfileState.ProfileError(ex.message ?: "Error saving profile")
                }
        }
    }

    fun deleteAccount(userId: String) {
        _profileState.value = ProfileState.Loading
        viewModelScope.launch {
            repo.deleteAccount(userId)
                .onSuccess {
                    _profileState.value = ProfileState.DeleteAccountSuccess
                }
                .onFailure { ex ->
                    _profileState.value = ProfileState.ProfileError(ex.message ?: "Error deleting account")
                }
        }
    }

    fun signOut() = repo.signOut()

    fun convertBitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
    }

    fun convertBase64ToBitmap(b64: String): Bitmap {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
