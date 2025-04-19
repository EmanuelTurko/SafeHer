package com.example.safeher.settings.profile

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import com.example.safeher.general.ErrorDialog
import com.example.safeher.general.LoadingDialog
import com.example.safeher.general.SuccessDialog
import com.example.safeher.general.showCustomToast
import com.example.safeher.general.showDatePicker
import com.example.safeher.settings.SettingsMainActivity
import com.example.safeher.settings.profile.profileViewModel.ProfileState
import com.example.safeher.settings.profile.profileViewModel.ProfileViewModel
import com.example.safeher.model.User
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser

class ProfileFragment : Fragment() {

    private var mAuth = FirebaseAuth.getInstance()
    private lateinit var saveButton: MaterialButton
    private lateinit var nameInput: TextInputEditText
    private lateinit var lastNameInput: TextInputEditText
    private lateinit var birthDateInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var oldPasswordInput: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilPasswordOld: TextInputLayout
    private lateinit var logoutButton: AppCompatImageButton
    private lateinit var addImage: AppCompatImageButton
    private lateinit var profileImage: AppCompatImageView
    private val viewModel: ProfileViewModel by viewModels()
    private var isAfterRegistrationScreen = false
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        try{
            val args = ProfileFragmentArgs.fromBundle(requireArguments())
            isAfterRegistrationScreen = args.isAfterRegistrationScreen

        } catch (e: Exception) {
            isAfterRegistrationScreen = false
        }
        initializeViews(view)
        setupClickListeners()
        if(isAfterRegistrationScreen) {
            setFieldsEditableState(true)
            passwordInput.visibility = View.GONE
            oldPasswordInput.visibility = View.GONE
            tilPasswordOld.visibility = View.GONE
            tilPassword.visibility = View.GONE
            logoutButton.visibility = View.GONE
            populateUserData()
        } else {
            loadingDialog?.show()
        }
        populateUserData()
        setupObservers()
        return view
    }

    private fun initializeViews(view : View) {
        saveButton = view.findViewById(R.id.saveButton)
        nameInput = view.findViewById(R.id.nameEditText)
//        birthDateInput = view.findViewById(R.id.etBirthDate)
//        emailInput = view.findViewById(R.id.etEmail)
//        passwordInput = view.findViewById(R.id.etPassword)
//        oldPasswordInput = view.findViewById(R.id.etOldPassword)
//        tilPasswordOld = view.findViewById(R.id.tilOldPassword)
//        tilPassword = view.findViewById(R.id.tilPassword)
        logoutButton = view.findViewById(R.id.logoutButton)
        profileImage = view.findViewById(R.id.ivProfile)
        addImage = view.findViewById(R.id.btnAddPhoto)
        loadingDialog = LoadingDialog(requireContext())
    }

    private fun setupClickListeners() {

        saveButton.setOnClickListener {
            handleSaveButtonClick()
        }

        logoutButton.setOnClickListener {
            handleLogout()
        }

        birthDateInput.setOnClickListener {
            showDatePicker { selectedDate ->
                birthDateInput.setText(selectedDate)
            }
        }

        addImage.setOnClickListener {
            openGallery()
        }
    }

    private fun handleSaveButtonClick() {
        saveUserData()
    }

    private fun setFieldsEditableState(isEditable: Boolean) {
        nameInput.isEnabled = isEditable
        if(isAfterRegistrationScreen) {
            emailInput.isEnabled = false
        } else {
            emailInput.isEnabled = isEditable
        }
        lastNameInput.isEnabled = isEditable
        birthDateInput.isEnabled = isEditable
        passwordInput.isEnabled = isEditable
        oldPasswordInput.isEnabled = isEditable
    }

    private fun populateUserData() {
        if(!isAfterRegistrationScreen) {
            viewModel.getUserData()
        }
        val currentUser = mAuth.currentUser
        currentUser?.let { user ->
            emailInput.setText(user.email)
            if(!isAfterRegistrationScreen) {
                passwordInput.setText("********")
                oldPasswordInput.setText("********")
            }
        }
    }


    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileState.SaveUserDataSuccess -> {
                    loadingDialog.dismiss()
                    if(isAfterRegistrationScreen){
                        activity?.startActivity(Intent(requireActivity(), SettingsMainActivity::class.java))
                    } else {
                        val customPopup = SuccessDialog(requireActivity())
                        customPopup.show(
                            "Success",
                            "Successfully update your personal information",
                            "Close"
                        )
                    }
                }
                is ProfileState.SaveUserDataError -> {
                    loadingDialog.dismiss()
                    val customPopup = ErrorDialog(requireActivity())
                    customPopup.show(
                        "Oops",
                        state.message,
                        "close"
                    )
                }
                is ProfileState.GetUserDataSuccess -> {
                    loadingDialog.dismiss()
                    if(state.user != null) {
                        val user = state.user
                        nameInput.setText(user.firstName)
                        lastNameInput.setText(user.lastName)
                        emailInput.setText(user.email)
                        birthDateInput.setText(user.birthDate)
                        if(!user.profileImage.isNullOrEmpty()) {
                            val imageBitmap = viewModel.convertBase64ToBitmap(user.profileImage)
                            profileImage.setImageBitmap(imageBitmap)
                        }
                    } else {
                        val customPopup = ErrorDialog(requireActivity())
                        customPopup.show(
                            "Oops..",
                            "Something went wrong, please try again later",
                            "close"
                        )
                    }
                }
                else -> {}
            }
        }
    }


    private fun saveUserData() {
        val currentUser = mAuth.currentUser
        val newName = nameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val birthDate = birthDateInput.text.toString().trim()
        var newEmail = emailInput.text.toString().trim()
        var profileImageString = ""
        val bitmap = profileImage.drawable.toBitmap()
        profileImageString = viewModel.convertBitmapToBase64(bitmap)


        if(newName.isEmpty() || lastName.isEmpty() || birthDate.isEmpty() || newEmail.isEmpty()) {
            showToast("Some fields are empty")
            return
        }
        val userData = User(id = currentUser?.uid ?:"" , firstName = newName,lastName =lastName, birthDate = birthDate, email = newEmail, profileImage =profileImageString)

        if(!isAfterRegistrationScreen) {
            newEmail = emailInput.text.toString().trim()
            val newPassword = passwordInput.text.toString()
            val oldPassword = oldPasswordInput.text.toString()

            currentUser?.let { user ->
                if (newEmail != user.email || newPassword != "********") {
                    if(oldPassword == "********") {
                        showToast("Please enter old password")
                        return
                    }
                    if(newPassword.isEmpty()) {
                        showToast("Password field is empty")
                        return
                    }
                    if (newPassword.length < 6) {
                        showCustomToast("Password must be at least 6 characters")
                        return
                    }

                    loadingDialog.show()
                    val credential = EmailAuthProvider.getCredential(user.email ?: "", oldPassword)
                    user.reauthenticate(credential)
                        .addOnCompleteListener { reAuthTask ->
                            if (reAuthTask.isSuccessful) {
                                performUserUpdates(user, newEmail, newPassword, userData)
                            } else {
                                loadingDialog.dismiss()
                                showToast("Authentication failed: ${reAuthTask.exception?.message}")
                            }
                        }
                } else {
                    updateUserProfile(userData)
                }
            }
        } else {
            updateUserProfile(userData)
        }
    }

    private fun updateUserProfile(user : User) {
        if(!loadingDialog.isShowing) {
            loadingDialog.show()
        }
        viewModel.saveUserData(user)
    }

    private fun performUserUpdates(user: FirebaseUser, newEmail: String, newPassword: String, userData: User) {
        if (newEmail != user.email) {
            viewModel.updateUserEmail(newEmail)
        }

        if (newPassword != "********") {
            viewModel.updateUserPassword(newPassword)
        }
        updateUserProfile(userData)
    }


    private fun showToast(message: String) {
        showCustomToast(message = message)
    }

    private fun handleLogout() {
        viewModel.signOut()
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }


/*****************************************************
    Handle with camera permission and Image Chooser bitmap
******************************************************/

    private fun openGallery() {
        // Check permissions before opening the chooser
        if (checkPermissions()) {
            openImageChooser()
        } else {
            requestPermissions()
        }
    }

    // Check if required permissions are granted
    private fun checkPermissions(): Boolean {
        val permissionCamera = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
        return permissionCamera == PackageManager.PERMISSION_GRANTED
    }

    // Request required permissions
    private fun requestPermissions() {
        requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
    }

    // Create the ActivityResultLauncher for picking an image
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.let {
                val selectedImage: Uri? = it.data
                if (selectedImage != null) {
                    // Handle the selected image from the gallery
//                    Picasso.get().load(selectedImage).into(profileImage)
                } else {
                    // Handle the camera image
                    val photo: Bitmap = it.extras?.get("data") as Bitmap
                    profileImage.setImageBitmap(photo)
                }
            }
        }
    }

    // Create the ActivityResultLauncher for requesting permissions
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allPermissionsGranted = permissions.entries.all { it.value }
        if (allPermissionsGranted) {
            openImageChooser()
        } else {
            showCustomToast( "Permissions denied, unable to choose image")
        }
    }

    // Open image chooser (camera or gallery)
    private fun openImageChooser() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        // Create a chooser to allow the user to select between camera and gallery
        val chooserIntent = Intent.createChooser(galleryIntent, "Select Image").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }

        pickImageLauncher.launch(chooserIntent)
    }
    /*********END***********/

}