package com.example.safeher.auth.authFragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import com.example.safeher.utils.setupUI

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

    private lateinit var citySpinner: Spinner

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
        requireActivity().setupUI(view)
        initializeViews()
        setupCitySpinner()
        setupClickListeners()

        val authRepository = AuthRepository(RetroFitClient.getApiService(requireContext()))
        val factory = AuthViewModelFactory(authRepository)
        viewModelApi = ViewModelProvider(this, factory)[AuthViewModelApi::class.java]

        registerObserver()
    }

    private fun initializeViews() {
        mFullName = binding?.fullNameEditText
        mEmail = binding?.emailEditTextRegister
        mPassword = binding?.passwordEditTextRegister
        mRegisterBtn = binding?.registerButton
        mMoveToLoginScreenBtn = binding?.loginText
        mPhone = binding?.phoneEditText
        citySpinner = binding?.citySpinner!!
        // mIdPhoto omitted/commented out since not used
    }

    private fun setupCitySpinner() {
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.israel_cities,
            R.layout.spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item
            )
            citySpinner.adapter = adapter
        }
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
        val fullName = mFullName?.text.toString().trim()
        val email = mEmail?.text.toString().trim()
        val password = mPassword?.text.toString()
        val phone = mPhone?.text.toString().trim()
        val cityName = citySpinner.selectedItem as String

        if (fullName.isEmpty() || !fullName.contains(" ")) {
            showCustomToast("Please enter your full name")
            return
        }

        if (!email.matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+\$"))) {
            showCustomToast("Please enter a valid email address")
            return
        }

        if (!phone.matches(Regex("^05[0-9]{8}\$"))) {
            showCustomToast("Please enter a valid phone number")
            return
        }

        if (password.length < 8) {
            showCustomToast("Password must be at least 8 characters long")
            return
        }

        if (cityName.isEmpty()) {
            showCustomToast("Please select your city")
            return
        }

        val request = RegisterRequest(
            fullName = fullName,
            email = email,
            password = password,
            phoneNumber = phone,
            idPhotoUrl = "",
            city = cityName
        )
        Log.d("RegisterFragment", ">>> Register payload: $request")

        val sharedPref = requireContext().getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
        sharedPref.edit().putString("fullName", fullName).apply()

        viewModelApi.registerUser(request)
    }

    private fun registerObserver() {
        viewModelApi.registerResponse.observe(viewLifecycleOwner) { response ->
            showLoadingState(false)

            if (response.error != null) {
                ErrorDialog(requireActivity()).show(
                    "Oops",
                    response.error,
                    "TRY AGAIN"
                )
                mRegisterBtn?.isEnabled = true
            } else {
                showCustomToast(
                    message = "Registration successful",
                    title = "Success",
                    iconResId = R.drawable.ic_check_circle
                )
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            }
        }
    }

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
