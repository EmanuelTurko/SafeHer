package com.example.safeher.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        binding?.backButtonCard?.setOnClickListener {
            findNavController().navigate(R.id.action_settingsSafeCircleFragment_to_mySafeCircleFragment)
        }

        val returnedSelected = arguments?.getParcelableArrayList<ContactItem>("selected_contacts")
        returnedSelected?.let {
            preSelectedContacts = it
        }

        initView()
        getContent()

        val searchEditText = binding?.pairSearchView?.findViewById<AutoCompleteTextView>(
            androidx.appcompat.R.id.search_src_text
        )
        searchEditText?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        searchEditText?.setHintTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        binding?.pairSearchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText)
                if (!newText.isNullOrEmpty()) {
                    val isRTL = newText.any { it in '֐'..'׿' }
                    searchEditText?.textDirection =
                        if (isRTL) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
                } else {
                    searchEditText?.textDirection = View.TEXT_DIRECTION_LTR
                }
                return true
            }
        })
    }

    private fun initView() {
        binding?.recyclerViewContacts?.layoutManager = LinearLayoutManager(requireContext())
        adapter = ContactAdapter(5, requireContext())
        binding?.recyclerViewContacts?.adapter = adapter

        binding?.finishButton?.setOnClickListener {
            val selected = adapter.getSelectedContacts()
            val selectedNumbers = selected.map { it.phoneNumber }

            if (selected.isNotEmpty()) {
                val sharedPref = requireContext().getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
                val fullName = sharedPref.getString("fullName", null) ?: ""

                Log.d("SafeCircleFragment", " Selected contacts: $selected")
                Log.d("SafeCircleFragment", " Full name from shared preferences: $fullName")

                safeCircleViewModel.updateUserSafeCircle(fullName, selected)

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val contactMaps = selected.map {
                        mapOf("name" to it.name, "phone" to normalizePhone(it.phoneNumber))
                    }
                    FirebaseFirestore.getInstance()
                        .collection("safe_circle")
                        .document(userId)
                        .set(mapOf("contacts" to contactMaps))
                        .addOnSuccessListener {
                            Log.d("SafeCircle", "Contacts saved to Firestore")
                        }
                        .addOnFailureListener {
                            Log.e("SafeCircle", "Failed to save contacts to Firestore", it)
                        }
                }
            }

            Toast.makeText(requireActivity(), "Selected: ${selected.joinToString { it.name }}", Toast.LENGTH_LONG).show()

            val bundle = Bundle().apply {
                putParcelableArrayList("selected_contacts", ArrayList(selected))
            }
            findNavController().navigate(R.id.action_settingsSafeCircleFragment_to_mySafeCircleFragment, bundle)
        }
    }

    private fun getContent() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                loadContacts()
            } else {
                Toast.makeText(requireActivity(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        checkContactPermission()
    }

    private fun checkContactPermission() {
        when {
            ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS) ==
                    PackageManager.PERMISSION_GRANTED -> {
                loadContacts()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_CONTACTS) -> {
                AlertDialog.Builder(requireActivity())
                    .setTitle("Permission Required")
                    .setMessage("We need access to your contacts to display them.")
                    .setPositiveButton("Allow") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                    .setNegativeButton("Deny", null)
                    .show()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    private fun loadContacts() {
        contactsList = mutableListOf()
        val cursor = context?.contentResolver?.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                val isSelected = preSelectedContacts?.any { it.phoneNumber == number } == true
                contactsList.add(ContactItem(name, number, isSelected))
            }
        }

        adapter.submitList(contactsList)
    }

    private fun filterContacts(query: String?) {
        if (query.isNullOrEmpty()) {
            adapter.submitList(contactsList)
        } else {
            val filteredList = contactsList.filter {
                it.name.contains(query, ignoreCase = true)
            }
            adapter.submitList(filteredList)
        }
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^\\d+]"), "")
            .replace("^0".toRegex(), "+972")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
