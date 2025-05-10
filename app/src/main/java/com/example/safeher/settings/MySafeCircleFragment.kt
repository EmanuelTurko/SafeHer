package com.example.safeher.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safeher.databinding.FragmentConfirmSafeCircleBinding
import com.example.safeher.model.ContactItem
import com.example.safeher.auth.safeCircle.adapter.ConfirmContactsAdapter
import com.example.safeher.R
import com.example.safeher.databinding.FragmentMySafeCircleBinding

class MySafeCircleFragment : Fragment() {

    private var _binding: FragmentMySafeCircleBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ConfirmContactsAdapter
    private var contactList: MutableList<ContactItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMySafeCircleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // טוען אנשי קשר שנשלחו מ־PairFragment
        arguments?.let {
            val safeContacts = it.getParcelableArrayList<ContactItem>("selected_contacts")
            contactList = safeContacts?.toMutableList() ?: mutableListOf()
        }

        adapter = ConfirmContactsAdapter(contactList) { removedContact ->
            contactList.remove(removedContact)
            adapter.notifyDataSetChanged()
        }

        binding.backButtonCard.setOnClickListener {
            findNavController().navigate(R.id.action_mySafeCircleFragment_to_settingsLobbyFragment)
        }

        binding.recyclerViewConfirm.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewConfirm.adapter = adapter


        binding.editButton.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelableArrayList("selected_contacts", ArrayList(contactList))
            }
            findNavController().navigate(R.id.action_mySafeCircleFragment_to_settingsSafeCircleFragment, bundle)
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
