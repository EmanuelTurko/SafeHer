package com.example.safeher.settings.profile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.safeher.R
import com.example.safeher.auth.MainActivity
import com.example.safeher.general.LoadingDialog
import com.example.safeher.general.showCustomToast
import com.example.safeher.model.ContactItem
import com.example.safeher.model.User
import com.example.safeher.settings.profile.profileViewModel.ProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import androidx.core.content.edit
import androidx.core.graphics.scale

class ProfileFragment : Fragment() {

    private lateinit var saveButton: MaterialButton
    private lateinit var removeAccountButton: MaterialButton

    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var phoneLayout: TextInputLayout

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText

    private lateinit var profileImage: AppCompatImageView
    private lateinit var loadingDialog: LoadingDialog

    private val viewModel: ProfileViewModel by viewModels()
    private var originalUser: User? = null
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("userId", "") ?: ""
        Log.d("ProfileFragment", "Loaded userId='$userId'")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        saveButton          = view.findViewById(R.id.saveButton)
        removeAccountButton = view.findViewById(R.id.removeAccountButton)

        nameLayout  = view.findViewById(R.id.nameInputLayout)
        emailLayout = view.findViewById(R.id.emailInputLayout)
        phoneLayout = view.findViewById(R.id.phoneInputLayout)

        nameInput  = view.findViewById(R.id.nameEditText)
        emailInput = view.findViewById(R.id.emailEditText)
        phoneInput = view.findViewById(R.id.phoneEditText)

        profileImage = view.findViewById(R.id.ivProfile)
        loadingDialog = LoadingDialog(requireContext())

        setupEndIconListeners()
        setupClickListeners()
        setupObservers()
        seedLocalFields()

        // Only fetch from server if we have no local cache yet
        val prefs = requireContext().getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        if (prefs.getString("fullName", null).isNullOrEmpty()) {
            viewModel.getUserData(userId)
        }

        return view
    }

    private fun setupEndIconListeners() {
        nameLayout.setEndIconOnClickListener {
            nameInput.isEnabled = true
            nameInput.isFocusableInTouchMode = true
            nameInput.requestFocus()
        }
        emailLayout.setEndIconOnClickListener {
            emailInput.isEnabled = true
            emailInput.isFocusableInTouchMode = true
            emailInput.requestFocus()
        }
        phoneLayout.setEndIconOnClickListener {
            phoneInput.isEnabled = true
            phoneInput.isFocusableInTouchMode = true
            phoneInput.requestFocus()
        }
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener { onSaveClicked() }
        removeAccountButton.setOnClickListener { onRemoveAccountClicked() }
    }

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            loadingDialog.dismiss()
            when (state) {
                ProfileState.Loading -> loadingDialog.show()

                is ProfileState.GetUserDataSuccess -> {
                    originalUser = state.user
                    state.user?.let { u ->
                        nameInput.setText(u.fullName)
                        emailInput.setText(u.email)
                        phoneInput.setText(u.phoneNumber)
                        u.profilePicture
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { b64 ->
                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                profileImage.setImageBitmap(
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                )
                            }
                    }
                    lockAllFields()
                    // also seed local so next entry uses cache
                    saveToLocalPrefs(
                        nameInput.text.toString(),
                        emailInput.text.toString(),
                        phoneInput.text.toString(),
                        originalUser?.profilePicture.orEmpty()
                    )
                }

                ProfileState.SaveUserDataSuccess -> {
                    showCustomToast("פרטי המשתמש נשמרו בהצלחה")
                    // update cache
                    saveToLocalPrefs(
                        nameInput.text.toString(),
                        emailInput.text.toString(),
                        phoneInput.text.toString(),
                        // picture already updated in prefs by our code below
                        requireContext().getSharedPreferences("userInfo", Context.MODE_PRIVATE)
                            .getString("profilePic", "") ?: ""
                    )
                    lockAllFields()
                }

                ProfileState.DeleteAccountSuccess -> {
                    requireContext()
                        .getSharedPreferences("auth", Context.MODE_PRIVATE)
                        .edit { clear() }
                    startActivity(
                        Intent(requireContext(), MainActivity::class.java)
                            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
                    )
                }

                is ProfileState.ProfileError -> {
                    showCustomToast("שגיאה: ${state.message}")
                    Log.e("ProfileFragment", state.message)
                }
            }
        }
    }

    private fun seedLocalFields() {
        val prefs = requireContext().getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        nameInput.setText(prefs.getString("fullName", ""))
        emailInput.setText(prefs.getString("email", ""))
        phoneInput.setText(prefs.getString("phoneNumber", ""))

        prefs.getString("profilePic", null)
            ?.takeIf { it.length > 100 }
            ?.let { b64 ->
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                profileImage.setImageBitmap(
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                )
            }

        lockAllFields()
    }

    private fun saveToLocalPrefs(name: String, email: String, phone: String, picBase64: String) {
        val prefs = requireContext().getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("fullName", name)
            putString("email", email)
            putString("phoneNumber", phone)
            putString("profilePic", picBase64)
            apply()
        }
    }

    private fun lockAllFields() {
        nameInput.isEnabled = false
        emailInput.isEnabled = false
        phoneInput.isEnabled = false
    }

    private fun onSaveClicked() {
        val bmp = profileImage.drawable?.toBitmap()
        val base64 = bmp?.let {
            val maxDim = 500
            val ratio = it.width.toFloat() / it.height
            val newW = if (it.width >= it.height) maxDim else (maxDim * ratio).toInt()
            val newH = if (it.width >= it.height) (maxDim / ratio).toInt() else maxDim
            val resized = it.scale(newW, newH)
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
            // also cache the new picture locally
            requireContext().getSharedPreferences("userInfo", Context.MODE_PRIVATE)
                .edit {
                    putString("profilePic", b64)
                }
            b64
        } ?: ""

        val contactsJson = requireContext()
            .getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
            .getString("safe_circle_contacts_$userId", "[]")
            .orEmpty()
        val contacts = Gson().fromJson(contactsJson, Array<ContactItem>::class.java).toList()

        val updatedUser = User(
            id                 = userId,
            fullName           = nameInput.text.toString().trim(),
            email              = emailInput.text.toString().trim(),
            password           = originalUser?.password.orEmpty(),
            phoneNumber        = phoneInput.text.toString().trim(),
            idPhotoUrl         = originalUser?.idPhotoUrl.orEmpty(),
            profilePicture     = base64,
            safeCircleContacts = contacts
        )
        viewModel.saveUserData(updatedUser)
    }

    private fun onRemoveAccountClicked() {
        viewModel.deleteAccount(userId)
    }
}
