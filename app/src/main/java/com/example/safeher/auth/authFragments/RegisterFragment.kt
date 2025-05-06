package com.example.safeher.auth.authFragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.activity.result.contract.ActivityResultContracts
import com.airbnb.lottie.LottieAnimationView
import com.example.safeher.R
import com.example.safeher.api.RetroFitClient
import com.example.safeher.api.auth.AuthRepository
import com.example.safeher.auth.authViewModel.AuthViewModel
import com.example.safeher.auth.authViewModel.AuthViewModelApi
import com.example.safeher.auth.authViewModel.AuthViewModelFactory
import com.example.safeher.databinding.FragmentRegisterBinding
import com.example.safeher.general.ErrorDialog
import com.example.safeher.general.showCustomToast
import com.example.safeher.model.RegisterRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.content.edit

class RegisterFragment : Fragment() {

    private var binding: FragmentRegisterBinding? = null
    private var mMoveToLoginScreenBtn: AppCompatTextView? = null
    private var mFullName: TextInputEditText? = null
    private var mPassword: TextInputEditText? = null
    private var mRegisterBtn: MaterialButton? = null
    private var mPhone: TextInputEditText? = null
    private var mEmail: TextInputEditText? = null
    private var mIdPhoto: TextInputEditText? = null
    private var mAnimationView: LottieAnimationView? = null
    private val viewModel: AuthViewModel by viewModels()

    private lateinit var viewModelApi: AuthViewModelApi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupClickListeners()

        val authRepository = AuthRepository(RetroFitClient.apiService)
        val factory = AuthViewModelFactory(authRepository)
        viewModelApi = ViewModelProvider(this, factory)[AuthViewModelApi::class.java]

        registerObserver()
    }

    private fun initializeViews(view: View) {
        mFullName = binding?.fullNameEditText
        mEmail = binding?.emailEditTextRegister
        mPassword = binding?.passwordEditTextRegister
        mRegisterBtn = binding?.registerButton
        mMoveToLoginScreenBtn = binding?.loginText
        mPhone = binding?.phoneEditText
        mIdPhoto = binding?.idPhotoEditText
        binding?.idPhotoInputLayout?.setOnClickListener { openGallery() }

    }

    private fun setupClickListeners() {
        mMoveToLoginScreenBtn?.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        mRegisterBtn?.setOnClickListener {
            registerUser()
        }
    }


    private fun showLoadingState(isLoading: Boolean) {
        mRegisterBtn?.isEnabled = !isLoading
    }

    private fun registerUser() {

        val request = RegisterRequest(
            fullName = mFullName?.text.toString().trim(),
            email = mEmail?.text.toString().trim(),
            password = mPassword?.text.toString(),
            phoneNumber = mPhone?.text.toString().trim(),
            idPhotoUrl = mIdPhoto?.text.toString().trim()
        )

        val sharedPref = requireContext().getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("fullName", request.fullName)
            apply()
        }
        viewModelApi.registerUser(request)

    }

    private fun registerObserver() {
        viewModelApi.registerResponse.observe(viewLifecycleOwner) { response ->
            showLoadingState(false)

            if (response.error != null) {
                val customProp = ErrorDialog(requireActivity())
                customProp.show(
                    "Oops",
                    response.error,
                    "TRY AGAIN"
                )
                mRegisterBtn?.isEnabled = true
            } else {
                showCustomToast("Registration successful")
                findNavController().navigate(R.id.action_registerFragment_to_safeCircleIntroFragment)

            }
        }
    }

    // מאפשר לבחור תמונה מהגלריה
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                mIdPhoto?.setText(it.toString())
            }
        }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
