package com.example.safeher.settings.safeCircle

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safeher.R
import com.example.safeher.api.RetroFitClient
import com.example.safeher.auth.safeCircle.SafeCircleViewModel
import com.example.safeher.auth.safeCircle.SafeCircleViewModelFactory
import com.example.safeher.auth.safeCircle.adapter.ConfirmContactsAdapter
import com.example.safeher.databinding.FragmentMySafeCircleBinding
import com.example.safeher.model.ContactItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MySafeCircleFragment : Fragment() {

    private var _binding: FragmentMySafeCircleBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ConfirmContactsAdapter
    private var contactList: MutableList<ContactItem> = mutableListOf()

    private val safeCircleViewModel: SafeCircleViewModel by lazy {
        val apiService = RetroFitClient.getApiService(requireContext())
        val factory = SafeCircleViewModelFactory(apiService)
        ViewModelProvider(this, factory)[SafeCircleViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMySafeCircleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle system Back press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBack()
            }
        )
        binding.backButtonCard.setOnClickListener { handleBack() }

        // Home button always navigates home
        binding.homeButtonCard.setOnClickListener {
            findNavController().navigate(
                R.id.action_mySafeCircleFragment_to_SOSHomeScreenFragment
            )
        }


        binding.editText.text = "EDIT"
        binding.editButton.setOnClickListener {
            // Navigate to edit-contacts screen with currently loaded list
            val bundle = Bundle().apply {
                putParcelableArrayList(
                    "selected_contacts",
                    ArrayList(contactList)
                )
            }
            findNavController().navigate(
                R.id.action_mySafeCircleFragment_to_settingsSafeCircleFragment,
                bundle
            )
        }

        initRecycler()
    }

    override fun onResume() {
        super.onResume()
        loadContactsFromPrefs()
        adapter.updateContacts(contactList)
    }

    private fun handleBack() {
        if (contactList.isEmpty()) {
            findNavController().navigate(
                R.id.action_mySafeCircleFragment_to_safeCircleIntroFragment
            )
        } else {
            findNavController().navigate(
                R.id.action_mySafeCircleFragment_to_settingsLobbyFragment
            )
        }
    }

    private fun initRecycler() {
        adapter = ConfirmContactsAdapter(contactList) { removedItem ->
            contactList.remove(removedItem)
            updateSharedPrefs()
            updateMongo()
            adapter.updateContacts(contactList)
        }
        binding.recyclerViewConfirm.layoutManager =
            LinearLayoutManager(requireContext())
        binding.recyclerViewConfirm.adapter = adapter
    }

    private fun loadContactsFromPrefs() {
        val authPrefs = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
        val currentUserId = authPrefs.getString("userId", "") ?: ""
        val prefs = requireContext()
            .getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("safe_circle_contacts_$currentUserId", "") ?: ""
        contactList = if (json.isNotEmpty()) {
            val type = object : TypeToken<List<ContactItem>>() {}.type
            Gson().fromJson<List<ContactItem>>(json, type).toMutableList()
        } else {
            mutableListOf()
        }
    }

    private fun updateSharedPrefs() {
        val authPrefs = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
        val currentUserId = authPrefs.getString("userId", "") ?: ""
        val prefs = requireContext()
            .getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("safe_circle_contacts_$currentUserId", Gson().toJson(contactList))
            .apply()
    }

    private fun updateMongo() {
        val fullName = requireContext()
            .getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
            .getString("fullName", "") ?: return
        safeCircleViewModel.updateUserSafeCircle(fullName, contactList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
