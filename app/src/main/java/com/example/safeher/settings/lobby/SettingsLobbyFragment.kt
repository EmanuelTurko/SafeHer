package com.example.safeher.settings.lobby

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.auth.MainActivity
import com.example.safeher.model.ContactItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SettingsLobbyFragment : Fragment() {

    private lateinit var mBackBtn: CardView
    private lateinit var mLogoutBtn: CardView
    private lateinit var mVideoLibraryOption: ConstraintLayout
    private lateinit var mPairOption: ConstraintLayout
    private lateinit var mProfileOption: ConstraintLayout
    private lateinit var mAboutOption: ConstraintLayout
    private lateinit var mQuestionsOption: ConstraintLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        mQuestionsOption = view.findViewById(R.id.qaOption)
    }

    private fun initListener() {
        mBackBtn.setOnClickListener {
            activity?.finish()
        }

        mAboutOption.setOnClickListener {
            findNavController().navigate(R.id.action_settingsLobbyFragment_to_aboutAppFragment)
        }

        mQuestionsOption.setOnClickListener {
            findNavController().navigate(R.id.action_settingsLobbyFragment_to_questionsAndAnswersFragment)
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
            val action =
                SettingsLobbyFragmentDirections.actionSettingsLobbyFragmentToVideoLibraryFragment2(
                    true
                )
            findNavController().navigate(action)
        }

        mPairOption.setOnClickListener {
            handleSafeCircleNavigation()
        }

        mProfileOption.setOnClickListener {
            findNavController().navigate(R.id.action_settingsLobbyFragment_to_profileFragment)
        }
    }

    private fun handleSafeCircleNavigation() {
        val authPrefs = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
        val currentUserId = authPrefs.getString("userId", "") ?: ""

        val sharedPref = requireContext()
            .getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
        val key = "safe_circle_contacts_${currentUserId}"
        val json = sharedPref.getString(key, null)

        Log.d("SafeCircleCheck", "Saved contacts for user $currentUserId: $json")

        val isEmptyList = json.isNullOrEmpty() || json == "[]"

        if (isEmptyList) {
            findNavController().navigate(
                R.id.action_settingsLobbyFragment_to_safeCircleIntroFragment
            )
        } else {
            val type = object : TypeToken<List<ContactItem>>() {}.type
            val selectedList: ArrayList<ContactItem> =
                Gson().fromJson(json, type) as ArrayList<ContactItem>

//            val bundle = Bundle().apply {
//                putParcelableArrayList("selected_contacts", selectedList)
//                putBoolean("showDone", true)
//            }
            findNavController().navigate(
                R.id.action_settingsLobbyFragment_to_mySafeCircleFragment,
//                bundle
            )
        }
    }
}

