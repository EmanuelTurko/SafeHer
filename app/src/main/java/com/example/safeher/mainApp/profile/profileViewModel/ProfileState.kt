package com.example.safeher.mainApp.profile.profileViewModel

import com.example.safeher.model.User

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object SaveUserDataSuccess : ProfileState()
    data class SaveUserDataError(val message: String) : ProfileState()
    data class GetUserDataSuccess(val user: User?) : ProfileState()
    data class GetUserDataError(val message: String?) : ProfileState()
    object UpdateEmailSuccess: ProfileState()
    data class UpdateEmailError(val message: String) : ProfileState()
    object UpdatePasswordSuccess : ProfileState()
    data class UpdatePasswordError(val message: String) : ProfileState()
    object UpdateUserSuccess : ProfileState()
    data class UpdateUserError(val message: String) : ProfileState()
}