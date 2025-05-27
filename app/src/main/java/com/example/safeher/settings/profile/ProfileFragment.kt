package com.example.safeher.settings.profile

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.auth.MainActivity
import com.example.safeher.databinding.FragmentProfileBinding
import com.example.safeher.general.LoadingDialog
import com.example.safeher.general.showCustomToast
import com.example.safeher.model.ContactItem
import com.example.safeher.model.User
import com.example.safeher.settings.profile.profileViewModel.ProfileViewModel
import com.google.gson.Gson
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var loadingDialog: LoadingDialog
    private var originalUser: User? = null
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog(requireContext())
        userId = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("userId", "") ?: ""
        Log.d("ProfileFragment", "Loaded userId = '$userId'")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nameEditText.isEnabled = false
        binding.emailEditText.isEnabled = false
        binding.phoneEditText.isEnabled = false

        setupListeners()
        setupObservers()
        seedLocalFields()
        viewModel.getUserData(userId)
    }

    private fun setupListeners() {
        binding.backButtonCard.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsLobbyFragment)
        }
        binding.homeButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_SOSHomeScreenFragment)
        }

        binding.nameInputLayout.setEndIconOnClickListener {
            binding.nameEditText.apply {
                isEnabled = true
                isFocusableInTouchMode = true
                requestFocus()
            }
        }
        binding.emailInputLayout.setEndIconOnClickListener {
            binding.emailEditText.apply {
                isEnabled = true
                isFocusableInTouchMode = true
                requestFocus()
            }
        }
        binding.phoneInputLayout.setEndIconOnClickListener {
            binding.phoneEditText.apply {
                isEnabled = true
                isFocusableInTouchMode = true
                requestFocus()
            }
        }

        binding.saveButton.setOnClickListener { onSaveClicked() }
        binding.removeButton.setOnClickListener { onRemoveAccountClicked() }
        binding.btnAddPhoto.setOnClickListener { openGallery() }
    }

    private fun setupObservers() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            loadingDialog.dismiss()
            when (state) {
                ProfileState.Loading -> loadingDialog.show()

                is ProfileState.GetUserDataSuccess -> {
                    originalUser = state.user
                    state.user?.let { user ->
                        binding.nameEditText.setText(user.fullName)
                        binding.phoneEditText.setText(user.phoneNumber)
                        binding.emailEditText.setText(user.email)
                        user.profilePicture
                            ?.takeIf(String::isNotEmpty)
                            ?.let { b64 ->
                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                binding.ivProfile.setImageBitmap(bmp)
                            }
                    }
                    binding.nameEditText.isEnabled = false
                    binding.emailEditText.isEnabled = false
                    binding.phoneEditText.isEnabled = false
                }

                ProfileState.SaveUserDataSuccess -> {
                    showCustomToast("פרטי המשתמש נשמרו בהצלחה")
                    saveToPrefs()
                    binding.nameEditText.isEnabled = false
                    binding.emailEditText.isEnabled = false
                    binding.phoneEditText.isEnabled = false
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
        val prefs = requireContext()
            .getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        binding.nameEditText.setText(prefs.getString("fullName", ""))
        binding.phoneEditText.setText(prefs.getString("phoneNumber", ""))
        binding.emailEditText.setText(prefs.getString("email", ""))
        prefs.getString("profilePic", "")?.takeIf { it.length > 100 }?.let { b64 ->
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.ivProfile.setImageBitmap(bmp)
        }
    }

    private fun onSaveClicked() {
        val newName  = binding.nameEditText.text.toString().trim()
        val newEmail = binding.emailEditText.text.toString().trim()
        val newPhone = binding.phoneEditText.text.toString().trim()

        if (newName.isEmpty() || newEmail.isEmpty()) {
            showCustomToast("אנא מלא/י שם ודוא\"ל")
            return
        }

        val bmp = binding.ivProfile.drawable?.toBitmap()
        val encoded = bmp?.let {
            val resized = Bitmap.createScaledBitmap(
                it, 500,
                (500f / it.width * it.height).toInt(), true
            )
            ByteArrayOutputStream().use { out ->
                resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
                Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
            }
        } ?: originalUser?.profilePicture.orEmpty()

        val contactsJson = requireContext()
            .getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
            .getString("safe_circle_contacts_$userId", "") ?: ""
        val contacts = if (contactsJson.isNotEmpty()) {
            Gson().fromJson(contactsJson, Array<ContactItem>::class.java).toList()
        } else emptyList()

        val user = User(
            id                 = userId,
            fullName           = newName,
            email              = newEmail,
            password           = originalUser?.password.orEmpty(),
            phoneNumber        = newPhone.takeIf(String::isNotEmpty)
                ?: originalUser?.phoneNumber.orEmpty(),
            idPhotoUrl         = originalUser?.idPhotoUrl.orEmpty(),
            profilePicture     = encoded,
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
            launchImageChooser()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.CAMERA)
            )
        }
    }

    private fun launchImageChooser() {
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val gallery = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        val chooser = Intent
            .createChooser(gallery, "בחר תמונה")
            .apply { putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera)) }
        pickImageLauncher.launch(chooser)
    }

    private val pickImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) binding.ivProfile.setImageURI(uri)
                else (result.data?.extras?.get("data") as? Bitmap)
                    ?.let { binding.ivProfile.setImageBitmap(it) }
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            if (perms[android.Manifest.permission.CAMERA] == true)
                launchImageChooser()
            else
                showCustomToast("לא ניתן לבחור תמונה ללא הרשאת מצלמה")
        }

    private fun saveToPrefs() {
        val prefs = requireContext()
            .getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("fullName", binding.nameEditText.text.toString().trim())
            putString("email",   binding.emailEditText.text.toString().trim())
            putString("phoneNumber", binding.phoneEditText.text.toString().trim())
            binding.ivProfile.drawable?.toBitmap()?.let {
                ByteArrayOutputStream().use { out ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, out)
                    putString("profilePic", Base64.encodeToString(out.toByteArray(), Base64.DEFAULT))
                }
            }
            apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
