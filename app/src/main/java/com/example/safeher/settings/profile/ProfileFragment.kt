package com.example.safeher.settings.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.safeher.R
import com.example.safeher.auth.MainActivity
import com.example.safeher.general.LoadingDialog
import com.example.safeher.general.showCustomToast
import com.example.safeher.model.User
import com.example.safeher.settings.profile.profileViewModel.ProfileState
import com.example.safeher.settings.profile.profileViewModel.ProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.example.safeher.utils.setupUI

class ProfileFragment : Fragment() {

    private lateinit var saveButton: MaterialButton
    private lateinit var removeAccountButton: MaterialButton
    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var addImage: AppCompatImageButton
    private lateinit var profileImage: AppCompatImageView
    private lateinit var loadingDialog: LoadingDialog

    private val viewModel: ProfileViewModel by viewModels()
    private var originalUser: User? = null
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve userId from the same pref where you saved it after login
        val prefs = requireContext().getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
        userId = prefs.getString("USER_ID", "") ?: ""
        Log.d("ProfileFragment", "Loaded userId = $userId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        initializeViews(view)
        setupClickListeners()
        setupObservers()
        //  Fetch the logged-in user's profile
        viewModel.getUserData(userId)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().setupUI(view)
    }

    private fun initializeViews(view: View) {
        saveButton          = view.findViewById(R.id.saveButton)
        removeAccountButton = view.findViewById(R.id.removeAccountButton)
        nameInput           = view.findViewById(R.id.nameEditText)
        emailInput          = view.findViewById(R.id.emailEditText)
        passwordInput       = view.findViewById(R.id.passwordEditText)
        profileImage        = view.findViewById(R.id.ivProfile)
        addImage            = view.findViewById(R.id.btnAddPhoto)
        loadingDialog       = LoadingDialog(requireContext())
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener { onSaveClicked() }
        removeAccountButton.setOnClickListener { onRemoveAccountClicked() }
        addImage.setOnClickListener { openGallery() }
    }

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileState.Loading -> loadingDialog.show()
                is ProfileState.GetUserDataSuccess -> {
                    loadingDialog.dismiss()
                    originalUser = state.user
                    state.user?.let { user ->
                        nameInput.setText(user.fullName)
                        emailInput.setText(user.email)
                        // If your User includes a Base64 profilePicture:
                        if (user.profilePicture.isNotEmpty()) {
                            val bmp = viewModel.convertBase64ToBitmap(user.profilePicture)
                            profileImage.setImageBitmap(bmp)
                        }
                    }
                }
                is ProfileState.SaveUserDataSuccess -> {
                    loadingDialog.dismiss()
                    showCustomToast("פרטי המשתמש נשמרו בהצלחה")
                }
                is ProfileState.ProfileError -> {
                    loadingDialog.dismiss()
                    showCustomToast(state.message)
                }
                else -> loadingDialog.dismiss()
            }
        }
    }

    private fun onSaveClicked() {
        val newName  = nameInput.text.toString().trim()
        val newEmail = emailInput.text.toString().trim()
        if (newName.isEmpty() || newEmail.isEmpty()) {
            showCustomToast("אנא מלאי שם ודוא\"ל")
            return
        }
        val bmp    = profileImage.drawable.toBitmap()
        val base64 = viewModel.convertBitmapToBase64(bmp)

        val user = User(
            id             = userId,
            fullName       = newName,
            email          = newEmail,
            password       = originalUser?.password ?: "",
            phoneNumber    = originalUser?.phoneNumber ?: "",
            birthDate      = originalUser?.birthDate,
            idPhotoUrl     = originalUser?.idPhotoUrl ?: "",
            profilePicture = base64
        )
        viewModel.saveUserData(user)
    }

    private fun onRemoveAccountClicked() {
        viewModel.signOut()
        startActivity(
            Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) profileImage.setImageURI(uri)
                else (result.data?.extras?.get("data") as? Bitmap)
                    ?.let { profileImage.setImageBitmap(it) }
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms[android.Manifest.permission.CAMERA] == true) openGallery()
            else showCustomToast("לא ניתן לבחור תמונה ללא הרשאת מצלמה")
        }

    private fun openGallery() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openImageChooser()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.CAMERA)
            )
        }
    }

    private fun openImageChooser() {
        val cameraIntent  = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        val chooser = Intent.createChooser(galleryIntent, "בחר תמונה").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }
        pickImageLauncher.launch(chooser)
    }
}
