package com.example.safeher.settings.profile.profileViewModel

import com.example.safeher.model.User

sealed class ProfileState {
    object Loading : ProfileState()
    data class GetUserDataSuccess(val user: User?) : ProfileState()
    object SaveUserDataSuccess : ProfileState()
    data class ProfileError(val message: String) : ProfileState()
}
