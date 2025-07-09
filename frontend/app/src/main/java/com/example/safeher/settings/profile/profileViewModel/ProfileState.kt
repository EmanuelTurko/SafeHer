import com.example.safeher.model.User

sealed class ProfileState {
    object Loading              : ProfileState()
    data class GetUserDataSuccess(val user: User?) : ProfileState()
    object SaveUserDataSuccess  : ProfileState()
    object DeleteAccountSuccess : ProfileState()       // ← newly added
    data class ProfileError(val message: String)       : ProfileState()
}
