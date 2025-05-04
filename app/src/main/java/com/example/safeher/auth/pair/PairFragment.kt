package com.example.safeher.auth.pair

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
import com.example.safeher.api.ApiService
import com.example.safeher.api.RetroFitClient
import com.example.safeher.auth.pair.adapter.ContactAdapter
import com.example.safeher.databinding.FragmentPairBinding
import com.example.safeher.model.ContactItem

class PairFragment : Fragment() {

    private var binding: FragmentPairBinding? = null
    private lateinit var adapter: ContactAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var contactsList = mutableListOf<ContactItem>()
    private val pairViewModel: PairViewModel by lazy {
        val apiService = RetroFitClient.apiService
        val factory = PairViewModelFactory(apiService)
        ViewModelProvider(this,factory)[PairViewModel::class.java]
    }
    private lateinit var safeCircleContact: List<String>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
         binding = FragmentPairBinding.inflate(inflater, container, false)

        initView()
        getContent()
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val searchEditText = binding?.pairSearchView?.findViewById<AutoCompleteTextView>(
            androidx.appcompat.R.id.search_src_text
        )
        searchEditText?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        searchEditText?.setHintTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))

        binding?.pairSearchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText)
                if (!newText.isNullOrEmpty()) {
                    val isRTL = newText.any { it in '\u0590'..'\u05FF' }

                    if (isRTL) {
                        searchEditText?.textDirection = View.TEXT_DIRECTION_RTL
                        searchEditText?.setPadding(16,
                            searchEditText.paddingTop, 16, searchEditText.paddingBottom
                        )
                    } else {
                        searchEditText?.textDirection = View.TEXT_DIRECTION_LTR
                        searchEditText?.setPadding(16,
                            searchEditText.paddingTop, 16, searchEditText.paddingBottom
                        )
                    }
                } else {
                    searchEditText?.textDirection = View.TEXT_DIRECTION_LTR
                }
                return true
            }
        })


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

    private fun initView() {
        binding?.recyclerViewContacts?.layoutManager = LinearLayoutManager(requireContext())
        adapter = ContactAdapter()
        binding?.recyclerViewContacts?.adapter = adapter

        /*binding?.backButtonCard?.setOnClickListener {
            findNavController().popBackStack()
        }*/
        binding?.finishButton?.setOnClickListener {
            val selected = adapter.getSelectedContacts()
            val selectedNumbers = selected.map { it.phoneNumber }
            if(selected.isNotEmpty()){
                val sharedPref = requireContext().getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
                val fullName = sharedPref.getString("fullName", null)?: ""
                Log.d("PairFragment", "Selected contacts: $selectedNumbers, fullName: $fullName")
                pairViewModel.updateUserSafeCircle(fullName, selectedNumbers)
            }
            Toast.makeText(requireActivity(), "Selected: ${selected.joinToString { it.name }}", Toast.LENGTH_LONG).show()
            //TODO save contacts in backend - send to server
            findNavController().navigate(R.id.action_PairFragment_to_loginFragment) //TODO maybe redirect to home?

        }

    }
    private fun loadContacts() {
        contactsList = mutableListOf<ContactItem>()
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
                contactsList.add(ContactItem(name,number))
            }
        }
        adapter.submitList(contactsList)

    }

    private fun checkContactPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadContacts()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.READ_CONTACTS
            ) -> {
                // Optional: show rationale
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
                // Request permission directly
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }
    private fun filterContacts(query: String?){
        if(query.isNullOrEmpty()){
            adapter.submitList(contactsList)
        } else{
            val filteredList = contactsList.filter { contact ->
                contact.name.contains(query, ignoreCase = true)
            }
            adapter.submitList(filteredList)
        }
    }


}