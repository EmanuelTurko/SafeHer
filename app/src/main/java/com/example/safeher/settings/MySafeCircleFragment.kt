package com.example.safeher.settings

import android.os.Bundle
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.safeher.utils.getStringShareRef
import com.example.safeher.utils.setStringShareRef
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

        initRecycler()

        binding.backButtonCard.setOnClickListener {
            findNavController().navigate(R.id.action_mySafeCircleFragment_to_settingsLobbyFragment)
        }

        binding.editButton.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelableArrayList("selected_contacts", ArrayList(contactList))
            }
            findNavController().navigate(
                R.id.action_mySafeCircleFragment_to_settingsSafeCircleFragment,
                bundle
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadContactsFromPrefs()
        adapter.updateContacts(contactList)
    }

    private fun initRecycler() {
        adapter = ConfirmContactsAdapter(contactList) { removedContact ->
            contactList.remove(removedContact)
            updateSharedPrefs()
            updateMongo()
            adapter.updateContacts(contactList)
        }
        binding.recyclerViewConfirm.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewConfirm.adapter = adapter
    }

    private fun loadContactsFromPrefs() {
        val json = requireContext().getStringShareRef("safe_circle_contacts", "safeher_prefs")
        if (json.isNotEmpty()) {
            val type = object : TypeToken<List<ContactItem>>() {}.type
            contactList = Gson().fromJson(json, type)
        }
    }

    private fun updateSharedPrefs() {
        val updatedJson = Gson().toJson(contactList)
        requireContext().setStringShareRef("safe_circle_contacts", updatedJson, "safeher_prefs")
    }

    private fun updateMongo() {
        val fullName = requireContext()
            .getSharedPreferences("CurrentUser", Context.MODE_PRIVATE)
            .getString("fullName", "") ?: return

        safeCircleViewModel.updateUserSafeCircle(fullName, contactList)
    }

    private fun normalizePhone(phone: String): String {
        return phone.replace(Regex("[^\\d+]"), "")
            .replace("^0".toRegex(), "+972")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
