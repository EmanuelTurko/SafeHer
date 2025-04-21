package com.example.safeher.auth.authFragments

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.example.safeher.R
import com.example.safeher.api.ApiService
import com.example.safeher.api.RetroFitClient
import com.example.safeher.api.auth.AuthRepository
import com.example.safeher.auth.authViewModel.AuthState
import com.example.safeher.auth.authViewModel.AuthViewModel
import com.example.safeher.auth.authViewModel.AuthViewModelApi
import com.example.safeher.auth.authViewModel.AuthViewModelFactory
import com.example.safeher.general.ErrorDialog
import com.example.safeher.general.showCustomToast
import com.example.safeher.model.RegisterRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterFragment : Fragment() {

    private var mMoveToLoginScreenBtn: AppCompatTextView? = null
    private var mFullName: TextInputEditText? = null
    private var mPassword: TextInputEditText? = null
    private var mRegisterBtn: MaterialButton? = null
    private var mPhone: TextInputEditText? = null
    //private var mIdPhotoUrl: TextInputEditText? = null
    private var mEmail: TextInputEditText? = null
    private var mAnimationView: LottieAnimationView? = null
    private val viewModel: AuthViewModel by viewModels()

    private lateinit var viewModelApi : AuthViewModelApi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupClickListeners()

        val authRepository = AuthRepository(RetroFitClient.apiService)

        val factory = AuthViewModelFactory(authRepository)
        viewModelApi = ViewModelProvider(this, factory)[AuthViewModelApi::class.java]

        //setupObservers()
        registerObserver()
    }

    private fun initializeViews(view: View) {
        mFullName = view.findViewById(R.id.usernameEditText)
        mEmail = view.findViewById(R.id.emailEditTextRegister)
        mPassword = view.findViewById(R.id.passwordEditTextRegister)
        mRegisterBtn = view.findViewById(R.id.registerButton)
        mMoveToLoginScreenBtn = view.findViewById(R.id.loginText)
//        mAnimationView = view.findViewById(R.id.registerAnimation)
//        mAnimationView?.playAnimation()
    }

    private fun setupClickListeners() {
        mMoveToLoginScreenBtn?.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        mRegisterBtn?.setOnClickListener {
            registerUser()
            //performRegistration()
        }
    }

    /*private fun performRegistration() {
        val username = mUsername?.text.toString().trim()
        val password = mPassword?.text.toString()

        if(validateInput(username, password)) {
            showLoadingState(true)
            viewModel.signUp(username, password)
        }
    }*/

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showCustomToast( "Invalid email address")
            isValid = false
        }

        if (password.isEmpty() || password.length < 6) {
            showCustomToast( "Password must be at least 6 characters")
            isValid = false
        }

        return isValid
    }

   /*private fun setupObservers() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {}
                is AuthState.Success -> {
                    //al action = RegisterFragmentDirections.actionRegisterFragmentToProfileFragment2(isAfterRegistrationScreen = true)
                    findNavController().navigate(R.id.loginFragment)
                }
                is AuthState.Error -> {
                    val customPopup = ErrorDialog(requireActivity())
                    customPopup.show(
                        "Oops",
                        state.message,
                        "TRY AGAIN"
                    )
                    mRegisterBtn?.isEnabled = true
                }
                else -> {}
            }
        }
    }*/

    private fun showLoadingState(isLoading: Boolean) {
        mRegisterBtn?.isEnabled = !isLoading
    }

    private fun registerUser() {
        val fullName = mFullName?.text.toString().trim()
        val email = mEmail?.text.toString().trim()
        val password = mPassword?.text.toString()
        val phoneNumber = mPhone?.text.toString().trim()
        //val idPhotoUrl = mIdPhotoUrl?.text.toString().trim()
        Log.d("RegisterFragment", "registerUser: $fullName, $email, $password, $phoneNumber")
        val request = RegisterRequest(
            fullName = fullName,
            email = email,
            password = password,
            phoneNumber = phoneNumber,
            idPhotoUrl = "" //idPhotoUrl,
        )
        Log.d("RegisterFragment", "registerUser: $request")
        viewModelApi.registerUser(request)
    }
    private fun registerObserver(){
        viewModelApi.registerResponse.observe(viewLifecycleOwner) { response ->
            showLoadingState(false)

            if(response.error != null){
                val customProp = ErrorDialog(requireActivity())
                customProp.show(
                    "Oops",
                    response.error,
                    "TRY AGAIN"
                )
                mRegisterBtn?.isEnabled = true
            } else {
                showCustomToast("Registration successful")
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }

}