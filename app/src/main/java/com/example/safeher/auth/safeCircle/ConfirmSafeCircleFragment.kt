package com.example.safeher.auth.safeCircle

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

class ConfirmSafeCircleFragment : Fragment() {

    private var _binding: FragmentConfirmSafeCircleBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ConfirmContactsAdapter
    private var contactList: MutableList<ContactItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfirmSafeCircleBinding.inflate(inflater, container, false)
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

        binding.recyclerViewConfirm.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewConfirm.adapter = adapter


        binding.finishButton.setOnClickListener {
            Toast.makeText(requireContext(), "Safe Circle confirmed!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_confirmSafeCircleFragment_to_SOS)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
