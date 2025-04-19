package com.example.safeher.auth.authFragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.example.safeher.R
import com.example.safeher.auth.authViewModel.AuthState
import com.example.safeher.auth.authViewModel.AuthViewModel
import com.example.safeher.general.ErrorDialog
import com.example.safeher.general.REMEMBER_MY_LOGIN
import com.example.safeher.general.SharedPrefsHelper
import com.example.safeher.general.SuccessDialog
import com.example.safeher.general.showCustomToast
import com.example.safeher.settings.SettingsMainActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginFragment : Fragment() {

    private lateinit var mMoveToRegisterScreenBtn: AppCompatTextView
    private lateinit var mLoginAnimationView: LottieAnimationView
    private lateinit var mUsername: TextInputEditText
    private lateinit var mPassword: TextInputEditText
    private lateinit var mLoginButton: MaterialButton
    private lateinit var mRememberMeCheckBox: AppCompatCheckBox
    private lateinit var mForgotPassword: AppCompatTextView
    private lateinit var mPasswordInputLayout: TextInputLayout
    private lateinit var mUsernameInputLayout: TextInputLayout
    var isForgotPassword = false
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        mLoginAnimationView.playAnimation()
        setupClickListeners()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {}
                is AuthState.ForgotPasswordSuccess -> {
                    val customPopup = SuccessDialog(requireActivity())
                    customPopup.show(
                        "Success",
                        "Password reset email sent",
                        "Close",
                        onButtonClick = {
                            isForgotPassword = false
                            forgotPasswordLogic(isForgotPassword)
                        }
                    )

                }
                is AuthState.ForgotPasswordError -> {
                    val customPopup = ErrorDialog(requireActivity())
                    customPopup.show(
                        "Oops",
                        state.error,
                        "TRY AGAIN"
                    )
                }
                is AuthState.Success -> {
                    if(mRememberMeCheckBox.isChecked) {
                        SharedPrefsHelper(requireContext()).save(REMEMBER_MY_LOGIN, true)
                    } else {
                        SharedPrefsHelper(requireContext()).save(REMEMBER_MY_LOGIN, false)
                    }
                    activity?.startActivity(Intent(requireActivity(), SettingsMainActivity::class.java))
                }
                is AuthState.Error -> {
                    val customPopup = ErrorDialog(requireActivity())
                    customPopup.show(
                        "Oops",
                        state.message,
                        "TRY AGAIN"
                    )
                }
                else -> {}
            }
        }
    }

    private fun initializeViews(view: View) {
        mMoveToRegisterScreenBtn = view.findViewById(R.id.signUpText)
        mLoginAnimationView = view.findViewById(R.id.welcomeAnimation)
        mUsername = view.findViewById(R.id.usernameEditText)
        mPassword = view.findViewById(R.id.passwordEditText)
        mLoginButton = view.findViewById(R.id.loginButton)
        mRememberMeCheckBox = view.findViewById(R.id.rememberMeCheckbox)
        mForgotPassword = view.findViewById(R.id.forgotPasswordText)
        mPasswordInputLayout = view.findViewById(R.id.passwordInputLayout)
        mUsernameInputLayout = view.findViewById(R.id.usernameInputLayout)
    }

    private fun setupClickListeners() {
        mMoveToRegisterScreenBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        mLoginButton.setOnClickListener {
            performLogin()
        }

        mForgotPassword.setOnClickListener {
            forgotPasswordLogic(!isForgotPassword)
        }
    }


    private fun forgotPasswordLogic(forgotClicked: Boolean) {
        isForgotPassword = forgotClicked
        var viewState = if (forgotClicked) View.INVISIBLE else View.VISIBLE
        mRememberMeCheckBox.visibility = viewState
        mPassword.visibility = viewState
        if(forgotClicked) mForgotPassword.text = "Back To Login" else mForgotPassword.text = "Forgot Password?"
        mPasswordInputLayout.visibility = viewState
        mUsername.text?.clear()
        if(forgotClicked) mUsernameInputLayout.hint = "Enter your mail" else mUsernameInputLayout.hint = "Username"
        if(forgotClicked) mLoginButton.text = "RESET PASSWORD" else mLoginButton.text = "LOGIN"
    }

    private fun performLogin() {
        val username = mUsername.text.toString().trim()
        val password = mPassword.text.toString()

        var vaild = if (isForgotPassword) viewModel.forgotPasswordValidateInput(email = username) else viewModel.validateInput(username,password)
        if (vaild.first) {
            if(isForgotPassword) {
                viewModel.forgotPassword(username)
            } else {
                viewModel.signIn(username, password)
            }
        } else {
            showCustomToast(vaild.second)
        }
    }
}