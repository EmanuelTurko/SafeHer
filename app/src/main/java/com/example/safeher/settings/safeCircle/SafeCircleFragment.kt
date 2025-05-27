package com.example.safeher.settings.safeCircle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safeher.R
import com.example.safeher.api.RetroFitClient
import com.example.safeher.auth.safeCircle.SafeCircleViewModel
import com.example.safeher.auth.safeCircle.SafeCircleViewModelFactory
import com.example.safeher.auth.safeCircle.adapter.ContactAdapter
import com.example.safeher.databinding.FragmentSettingsSafeCircleBinding
import com.example.safeher.model.ContactItem
import com.google.gson.Gson

class SafeCircleFragment : Fragment() {

    private var binding: FragmentSettingsSafeCircleBinding? = null
    private lateinit var adapter: ContactAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val contactsList = mutableListOf<ContactItem>()

    private val safeCircleViewModel: SafeCircleViewModel by lazy {
        val apiService = RetroFitClient.getApiService(requireContext())
        val factory = SafeCircleViewModelFactory(apiService)
        ViewModelProvider(this, factory)[SafeCircleViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsSafeCircleBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // intercept system back → conditional navigation
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBack()
            }
        )

        // UI back arrow uses same logic
        binding?.backButtonCard?.setOnClickListener { handleBack() }

        initView()
        setupPermissionLauncher()
        requestContactsPermission()
    }

    private fun handleBack() {
        val selected = adapter.getSelectedContacts()
        if (selected.isEmpty()) {
            // none chosen → pop back to intro
            findNavController().popBackStack()
        } else {
            val bundle = Bundle().apply {
                putParcelableArrayList("selected_contacts", ArrayList(selected))
                putBoolean("showDone", true)
            }
            findNavController().navigate(
                R.id.action_settingsSafeCircleFragment_to_mySafeCircleFragment,
                bundle
            )
        }
    }

    private fun initView() {
        binding?.recyclerViewContacts?.layoutManager = LinearLayoutManager(requireContext())
        adapter = ContactAdapter(5, requireContext())
        binding?.recyclerViewContacts?.adapter = adapter

        binding?.finishButton?.setOnClickListener {
            val selected = adapter.getSelectedContacts()
            // 1. Persist selection locally
            val authPrefs = requireContext()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
            val currentUserId = authPrefs.getString("userId", "") ?: ""
            val prefs = requireContext()
                .getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
            val key = "safe_circle_contacts_$currentUserId"
            prefs.edit().putString(key, Gson().toJson(selected)).apply()

            // 2. Update backend
            val fullName = requireContext()
                .getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
                .getString("fullName", "") ?: ""
            safeCircleViewModel.updateUserSafeCircle(fullName, selected)

            Toast.makeText(
                requireActivity(),
                "Selected: ${selected.joinToString { it.name }}",
                Toast.LENGTH_LONG
            ).show()

            // 3. Navigate to MySafeCircle
            val bundle = Bundle().apply {
                putParcelableArrayList("selected_contacts", ArrayList(selected))
                putBoolean("showDone", true)
            }
            findNavController().navigate(
                R.id.action_settingsSafeCircleFragment_to_mySafeCircleFragment,
                bundle
            )
        }
    }

    private fun setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) loadContacts()
            else Toast.makeText(
                requireContext(),
                "Permission to read contacts was denied.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestContactsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> loadContacts()
            else -> requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun loadContacts() {
        contactsList.clear()
        val cursor = requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val number = it.getString(numIdx) ?: continue
                contactsList.add(ContactItem(name, number))
            }
        }
        adapter.submitList(contactsList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
