package com.example.safeher.auth.authFragments

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.example.safeher.auth.authFragments.RegisterFragmentDirections
import com.example.safeher.R
import com.example.safeher.auth.authViewModel.AuthState
import com.example.safeher.auth.authViewModel.AuthViewModel
import com.example.safeher.general.ErrorDialog
import com.example.safeher.general.showCustomToast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlin.getValue

class RegisterFragment : Fragment() {

    private var mMoveToLoginScreenBtn: AppCompatTextView? = null
    private var mUsername: TextInputEditText? = null
    private var mPassword: TextInputEditText? = null
    private var mRegisterBtn: MaterialButton? = null
    private var mAnimationView: LottieAnimationView? = null
    private val viewModel: AuthViewModel by viewModels()

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
        setupObservers()
    }

    private fun initializeViews(view: View) {
        mUsername = view.findViewById(R.id.emailEditTextRegister)
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
            performRegistration()
        }
    }

    private fun performRegistration() {
        val username = mUsername?.text.toString().trim()
        val password = mPassword?.text.toString()

        if(validateInput(username, password)) {
            showLoadingState(true)
            viewModel.signUp(username, password)
        }
    }

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

    private fun setupObservers() {
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
    }

    private fun showLoadingState(isLoading: Boolean) {
        mRegisterBtn?.isEnabled = !isLoading
    }

}