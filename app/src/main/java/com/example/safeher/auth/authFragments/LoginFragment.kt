package com.example.safeher.auth.authFragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.example.safeher.R
import com.example.safeher.api.RetroFitClient
import com.example.safeher.api.auth.AuthRepository
import com.example.safeher.auth.authViewModel.AuthState
import com.example.safeher.auth.authViewModel.AuthViewModelApi
import com.example.safeher.auth.authViewModel.AuthViewModelFactory
import com.example.safeher.databinding.FragmentLoginBinding
import com.example.safeher.general.ErrorDialog
import com.example.safeher.general.REMEMBER_MY_LOGIN
import com.example.safeher.general.SharedPrefsHelper
import com.example.safeher.general.SuccessDialog
import com.example.safeher.home_screen.HomeScreenActivity
import com.example.safeher.model.LoginRequest
import com.example.safeher.utils.setStringShareRef
import com.example.safeher.utils.setupUI
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {

    private var binding: FragmentLoginBinding? = null
    private lateinit var viewModelApi: AuthViewModelApi
    private lateinit var mLoginAnimationView: LottieAnimationView
    private var mEmail: TextInputEditText? = null
    private var mPassword: TextInputEditText? = null
    private var isForgotPassword = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().setupUI(view)
        initializeViews()
        binding?.welcomeAnimation?.playAnimation()

        val authRepository = AuthRepository(RetroFitClient.getApiService(requireContext()))
        val factory = AuthViewModelFactory(authRepository)
        viewModelApi = ViewModelProvider(this, factory)[AuthViewModelApi::class.java]

        setupClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModelApi.loginResponse.observe(viewLifecycleOwner) { response ->
            if (response.message == "Successfully logged in" && response.data != null) {
                val rememberMe = binding?.rememberMeCheckbox?.isChecked
                val token = response.data.accessToken
                context?.getSharedPreferences("auth", Context.MODE_PRIVATE)?.edit()?.putString("token", token)?.apply()
                context?.getSharedPreferences("auth", Context.MODE_PRIVATE)?.edit()?.putString("userId", response.data.id)?.apply()

                requireContext().setStringShareRef("fullName", response.data.fullName, "userInfo")
                requireContext().setStringShareRef("email", response.data.email, "userInfo")
                requireContext().setStringShareRef("phoneNumber", response.data.phoneNumber, "userInfo")
                requireContext().setStringShareRef("profilePic", response.data.profilePicture.toString(), "userInfo")

                SharedPrefsHelper(requireContext()).save(REMEMBER_MY_LOGIN, rememberMe)
                startActivity(Intent(requireActivity(), HomeScreenActivity::class.java))
                requireActivity().finish()
            } else if (response.error != null) {
                ErrorDialog(requireActivity()).show("Oops", response.error, "TRY AGAIN")
            } else {
                ErrorDialog(requireActivity()).show("Oops", "Unknown error", "TRY AGAIN")
            }
        }

        viewModelApi.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.ForgotPasswordSuccess -> {
                    SuccessDialog(requireActivity()).show(
                        title = "Success",
                        message = "A password reset link was sent to your email",
                        buttonText = "OK"
                    )
                    forgotPasswordLogic(false)
                }
                is AuthState.ForgotPasswordError -> {
                    ErrorDialog(requireActivity()).show(
                        title = "Oops",
                        message = state.error,
                        buttonText = "OK"
                    )
                }
                else -> {}
            }
        }
    }

    private fun initializeViews() {
        mEmail = binding?.emailEditText
        mPassword = binding?.passwordEditText
    }

    private fun setupClickListeners() {
        binding?.signUpText?.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding?.loginButton?.setOnClickListener {
            val email = mEmail?.text.toString().trim()

            if (isForgotPassword) {
                val (isValid, errorMsg) = viewModelApi.forgotPasswordValidateInput(email)
                if (isValid) {
                    viewModelApi.forgotPassword(email)
                } else {
                    ErrorDialog(requireActivity()).show("Oops", errorMsg, "OK")
                }
            } else {
                performLogin()
            }
        }

        binding?.forgotPasswordText?.setOnClickListener {
            forgotPasswordLogic(!isForgotPassword)
        }
    }

    private fun forgotPasswordLogic(forgotClicked: Boolean) {
        isForgotPassword = forgotClicked
        val viewState = if (forgotClicked) View.INVISIBLE else View.VISIBLE

        binding?.rememberMeCheckbox?.visibility = viewState
        binding?.passwordEditText?.visibility = viewState
        binding?.passwordInputLayout?.visibility = viewState

        binding?.forgotPasswordText?.text = if (forgotClicked) "Back To Login" else "Forgot Password?"
        binding?.emailInputLayout?.hint = if (forgotClicked) "Enter your email" else "Email"
        binding?.loginButton?.text = if (forgotClicked) "RESET PASSWORD" else "LOGIN"
        binding?.emailEditText?.text?.clear()
    }

    private fun performLogin() {
        val request = LoginRequest(
            email = mEmail?.text.toString().trim(),
            password = mPassword?.text.toString()
        )
        Log.d("LoginFragment", "Attempting login with email: ${request.email}")
        viewModelApi.loginUser(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
