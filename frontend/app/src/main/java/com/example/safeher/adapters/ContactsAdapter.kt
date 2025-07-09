package com.example.safeher.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.databinding.ItemContactBinding
import com.example.safeher.model.Contact

class ContactsAdapter(
    private val contacts: List<Contact>,
    private val onCheckedChange: (Contact, Boolean) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun getItemCount() = contacts.size

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.binding.contactName.text = contact.name

        // מניעת קריאה כפולה של onCheckedChange
        holder.binding.checkbox.setOnCheckedChangeListener(null)
        holder.binding.checkbox.isChecked = contact.isChecked
        holder.binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            contact.isChecked = isChecked
            onCheckedChange(contact, isChecked)
        }
    }
}