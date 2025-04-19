package com.example.safeher.settings.lobby

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safeher.R

class SettingsLobbyFragment : Fragment() {

    lateinit var mBackBtn: CardView
    lateinit var mLogoutBtn: CardView
    lateinit var mVideoLibraryOption: ConstraintLayout
    lateinit var mPairOption: ConstraintLayout


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
    }

    private fun initListener() {
        mBackBtn.setOnClickListener {
            activity?.finish()
        }

        mLogoutBtn.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("LOGOUT_SUCCESS", true)
            }
            activity?.setResult(Activity.RESULT_OK, resultIntent)
            activity?.finish()
        }

        mVideoLibraryOption.setOnClickListener {
            val action = SettingsLobbyFragmentDirections.actionSettingsLobbyFragmentToVideoLibraryFragment2(true)
            findNavController().navigate(action)
        }

        mPairOption.setOnClickListener {
            findNavController().navigate(R.id.action_settingsLobbyFragment_to_pairFragment)
        }
    }
}