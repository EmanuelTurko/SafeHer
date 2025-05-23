package com.example.safeher.settings.profile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.auth.MainActivity
import com.example.safeher.general.LoadingDialog
import com.example.safeher.general.showCustomToast
import com.example.safeher.model.ContactItem
import com.example.safeher.model.User
import com.example.safeher.settings.profile.profileViewModel.ProfileViewModel
import com.example.safeher.utils.setupUI
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private lateinit var saveButton: MaterialButton
    private lateinit var mHome: CardView
    private lateinit var mBackBtn: CardView
    private lateinit var removeAccountButton: MaterialButton

    // layouts for end‐icon
    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var phoneLayout: TextInputLayout

    private lateinit var nameInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText

    private lateinit var addImage: AppCompatImageButton
    private lateinit var profileImage: AppCompatImageView
    private lateinit var loadingDialog: LoadingDialog

    private val viewModel: ProfileViewModel by viewModels()
    private var originalUser: User? = null
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authPrefs = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
        userId = authPrefs.getString("userId", "") ?: ""
        Log.d("ProfileFragment", "Loaded userId = '$userId'")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        initializeViews(view)
        initListener()
        setupEndIconListeners()
        setupClickListeners()
        setupObservers()
        seedLocalFields()
        viewModel.getUserData(userId)
        return view
    }

    private fun initListener() {
        mBackBtn.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsLobbyFragment)
        }
        mHome.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_SOSHomeScreenFragment)
        }
    }

    private fun initializeViews(view: View) {
        mHome = view.findViewById(R.id.homeButtonCard)
        mBackBtn = view.findViewById(R.id.backButtonCard)

        saveButton          = view.findViewById(R.id.saveButton)
        removeAccountButton = view.findViewById(R.id.removeAccountButton)

        // bind layouts
        nameLayout  = view.findViewById(R.id.nameInputLayout)
        emailLayout = view.findViewById(R.id.emailInputLayout)
        phoneLayout = view.findViewById(R.id.phoneInputLayout)

        // bind inputs
        nameInput   = view.findViewById(R.id.nameEditText)
        phoneInput  = view.findViewById(R.id.phoneEditText)
        emailInput  = view.findViewById(R.id.emailEditText)

        profileImage        = view.findViewById(R.id.ivProfile)
        profileImage.setImageResource(R.drawable.profile)
        addImage            = view.findViewById(R.id.btnAddPhoto)
        loadingDialog       = LoadingDialog(requireContext())

        // disable inputs initially
        nameInput.isEnabled = false
        emailInput.isEnabled = false
        phoneInput.isEnabled = false
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
        addImage.setOnClickListener { openGallery() }
    }

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            loadingDialog.dismiss()
            when (state) {
                ProfileState.Loading -> loadingDialog.show()

                is ProfileState.GetUserDataSuccess -> {
                    originalUser = state.user
                    state.user?.let { user ->
                        nameInput.setText(user.fullName)
                        phoneInput.setText(user.phoneNumber)
                        emailInput.setText(user.email)
                        user.profilePicture
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { b64 ->
                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                profileImage.setImageBitmap(
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                )
                            }
                    }
                    // lock after loading
                    nameInput.isEnabled = false
                    emailInput.isEnabled = false
                    phoneInput.isEnabled = false
                }

                ProfileState.SaveUserDataSuccess -> {
                    showCustomToast("פרטי המשתמש נשמרו בהצלחה")
                    // update local prefs
                    val prefs = requireContext()
                        .getSharedPreferences("userInfo", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("fullName", nameInput.text.toString().trim())
                        putString("email",   emailInput.text.toString().trim())
                        putString("phoneNumber", phoneInput.text.toString().trim())
                        profileImage.drawable?.toBitmap()?.let {
                            val out = ByteArrayOutputStream()
                            it.compress(Bitmap.CompressFormat.PNG, 100, out)
                            putString("profilePic", Base64.encodeToString(out.toByteArray(), Base64.DEFAULT))
                        }
                        apply()
                    }
                    // lock fields again
                    nameInput.isEnabled = false
                    emailInput.isEnabled = false
                    phoneInput.isEnabled = false

                    // optionally re-fetch or not...
                }

                ProfileState.DeleteAccountSuccess -> {
                    startActivity(
                        Intent(requireContext(), MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
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
        val local = requireContext()
            .getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        nameInput.setText(local.getString("fullName",""))
        phoneInput.setText(local.getString("phoneNumber",""))
        emailInput.setText(local.getString("email",""))
        local.getString("profilePic","")?.takeIf { it.length > 100 }?.let { b64 ->
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            profileImage.setImageBitmap(
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            )
        }
    }

    private fun onSaveClicked() {
        val newName  = nameInput.text.toString().trim()
        val newEmail = emailInput.text.toString().trim()
        val newPhone = phoneInput.text.toString().trim()
        if (newName.isEmpty() || newEmail.isEmpty()) {
            showCustomToast("אנא מלא/י שם ודוא\"ל")
            return
        }

        val bmp = profileImage.drawable?.toBitmap()
        val base64 = bmp?.let {
            val resized = Bitmap.createScaledBitmap(it, 500, (500f / it.width * it.height).toInt(), true)
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        } ?: originalUser?.profilePicture.orEmpty()

        val prefs        = requireContext().getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
        val jsonContacts = prefs.getString("safe_circle_contacts_$userId", "") ?: ""
        val contacts = if (jsonContacts.isNotEmpty()) {
            Gson().fromJson(jsonContacts, Array<ContactItem>::class.java).toList()
        } else emptyList()

        val user = User(
            id                 = userId,
            fullName           = newName,
            email              = newEmail,
            password           = originalUser?.password.orEmpty(),
            phoneNumber        = if (newPhone.isNotEmpty()) newPhone else originalUser?.phoneNumber.orEmpty(),
            idPhotoUrl         = originalUser?.idPhotoUrl.orEmpty(),
            profilePicture     = base64,
            safeCircleContacts = contacts
        )
        viewModel.saveUserData(user)
    }

    private fun onRemoveAccountClicked() {
        viewModel.deleteAccount(userId)
    }

    private fun openGallery() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openImageChooser()
        } else {
            requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))
        }
    }

    private fun openImageChooser() {
        val cameraIntent  = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        val chooser = Intent.createChooser(galleryIntent, "בחר תמונה")
            .apply { putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent)) }
        pickImageLauncher.launch(chooser)
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
}
