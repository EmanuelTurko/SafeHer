package com.example.safeher.settings.pair

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.model.ContactItem
import com.example.safeher.settings.pair.adapter.ContactAdapter

class PairFragment : Fragment() {

    lateinit var mBackBtn: CardView
    lateinit var mFinishBtn: CardView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pair, container, false)
        initView(view)
        initListener()
        getContent()
        return view
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

    private fun initView(view: View) {
        mBackBtn = view.findViewById(R.id.backButtonCard)
        mFinishBtn = view.findViewById(R.id.finishButton)
        recyclerView = view.findViewById(R.id.recyclerViewContacts)

    }

    private fun initListener() {
        mBackBtn.setOnClickListener {
            findNavController().navigate(R.id.action_pairFragment_to_settingsLobbyFragment)
        }

        mFinishBtn.setOnClickListener {
            val selected = adapter.getSelectedContacts()
            // TODO: Handle selected contacts (e.g., show Toast or move to next screen)
            Toast.makeText(requireActivity(), "Selected: ${selected.joinToString { it.name }}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadContacts() {
        val contactsList = mutableListOf<ContactItem>()
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

        adapter = ContactAdapter(contactsList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        for (contact in contactsList) {
            Log.d("Contact", contact.name)
        }
    }

    private fun checkContactPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadContacts()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                android.Manifest.permission.READ_CONTACTS
            ) -> {
                // Optional: show rationale
                AlertDialog.Builder(requireActivity())
                    .setTitle("Permission Required")
                    .setMessage("We need access to your contacts to display them.")
                    .setPositiveButton("Allow") { _, _ ->
                        requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                    }
                    .setNegativeButton("Deny", null)
                    .show()
            }

            else -> {
                // Request permission directly
                requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            }
        }
    }


}