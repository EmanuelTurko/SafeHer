// SafeCircleFragment.kt
package com.example.safeher.settings.safeCircle

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
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
    private var contactsList = mutableListOf<ContactItem>()
    private var preSelectedContacts: List<ContactItem>? = null

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

        // Back nav
        binding?.backButtonCard?.setOnClickListener {
            findNavController().navigate(R.id.action_settingsSafeCircleFragment_to_mySafeCircleFragment)
        }

        // If coming back with edited contacts
        preSelectedContacts = arguments
            ?.getParcelableArrayList<ContactItem>("selected_contacts")

        initView()
        requestContactsPermission()
        setupSearch()
    }

    private fun initView() {
        binding?.recyclerViewContacts?.layoutManager =
            LinearLayoutManager(requireContext())
        adapter = ContactAdapter(5, requireContext())
        binding?.recyclerViewContacts?.adapter = adapter

        binding?.finishButton?.setOnClickListener {
            val selected = adapter.getSelectedContacts()

            // 1. Pull currentUserId
            val authPrefs = requireContext()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
            val currentUserId = authPrefs.getString("userId", "") ?: ""

            // 2. Save under per-user key
            val prefs = requireContext()
                .getSharedPreferences("safeher_prefs", Context.MODE_PRIVATE)
            val key = "safe_circle_contacts_$currentUserId"
            val json = Gson().toJson(selected)
            prefs.edit().putString(key, json).apply()

            // 3. Update backend
            val fullName = requireContext()
                .getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
                .getString("fullName", "") ?: ""
            safeCircleViewModel.updateUserSafeCircle(fullName, selected)

            Toast.makeText(
                requireActivity(),
                "Selected: ${selected.joinToString { it.name }}",
                Toast.LENGTH_LONG
            ).show()

            // 4. Navigate back with flag
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

    private fun setupSearch() {
        val searchEditText = binding?.pairSearchView
            ?.findViewById<AutoCompleteTextView>(
                androidx.appcompat.R.id.search_src_text
            )
        searchEditText?.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.black)
        )
        searchEditText?.setHintTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.black)
        )
        binding?.pairSearchView?.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText)
                val isRTL = newText?.any { it in '֐'..'׿' } == true
                searchEditText?.textDirection = if (isRTL)
                    View.TEXT_DIRECTION_RTL
                else
                    View.TEXT_DIRECTION_LTR
                return true
            }
        })
    }

    private fun requestContactsPermission() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) loadContacts()
            else Toast.makeText(
                requireActivity(),
                "Permission denied",
                Toast.LENGTH_SHORT
            ).show()
        }
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> loadContacts()

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.READ_CONTACTS
            ) -> AlertDialog.Builder(requireActivity())
                .setTitle("Permission Required")
                .setMessage("We need access to your contacts to display them.")
                .setPositiveButton("Allow") { _, _ ->
                    requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
                .setNegativeButton("Deny", null)
                .show()

            else -> requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun loadContacts() {
        contactsList = mutableListOf()
        val cursor = requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val numIdx = it.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            while (it.moveToNext()) {
                val name = it.getString(nameIdx)
                val num = it.getString(numIdx)
                val isSel = preSelectedContacts
                    ?.any { it.phoneNumber == num } == true
                contactsList.add(ContactItem(name, num, isSel))
            }
        }
        adapter.submitList(contactsList)
    }

    private fun filterContacts(query: String?) {
        val filtered = if (query.isNullOrBlank()) {
            contactsList
        } else {
            contactsList.filter {
                it.name.contains(query, true) ||
                        it.phoneNumber.contains(query)
            }
        }
        adapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
