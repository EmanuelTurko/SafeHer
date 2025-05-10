package com.example.safeher.settings.lobby

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import androidx.core.content.edit
import com.example.safeher.auth.MainActivity

class SettingsLobbyFragment : Fragment() {

    lateinit var mBackBtn: CardView
    lateinit var mLogoutBtn: CardView
    lateinit var mVideoLibraryOption: ConstraintLayout
    lateinit var mPairOption: ConstraintLayout
    lateinit var mProfileOption: ConstraintLayout
    lateinit var mAboutOption: ConstraintLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        initView(view)
        initListener()
        return view
    }

    private fun initView(view: View) {
        mBackBtn = view.findViewById(R.id.backButtonCard)
        mLogoutBtn = view.findViewById(R.id.logoutButton)
        mVideoLibraryOption = view.findViewById(R.id.videoLibraryOption)
        mPairOption = view.findViewById(R.id.pairOption)
        mProfileOption = view.findViewById(R.id.profileOption)
        mAboutOption = view.findViewById(R.id.aboutOption)
    }

    private fun initListener() {
        mBackBtn.setOnClickListener {
            activity?.finish()
        }

        mAboutOption.setOnClickListener {
            findNavController().navigate(R.id.action_settingsLobbyFragment_to_aboutAppFragment)
        }

        mLogoutBtn.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            val clearContacts = requireContext().getSharedPreferences("or", Context.MODE_PRIVATE)

            prefs.edit {
                clear()
                apply()
            }

            clearContacts.edit {
                clear()
                apply()
            }

            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        mVideoLibraryOption.setOnClickListener {
            val action = SettingsLobbyFragmentDirections.actionSettingsLobbyFragmentToVideoLibraryFragment2(true)
            findNavController().navigate(action)
        }

        mPairOption.setOnClickListener {
            findNavController().navigate(R.id.action_settingsLobbyFragment_to_settingsSafeCircleFragment)
        }

        mProfileOption.setOnClickListener {
            findNavController().navigate(R.id.action_settingsLobbyFragment_to_profileFragment)
        }
    }
}
