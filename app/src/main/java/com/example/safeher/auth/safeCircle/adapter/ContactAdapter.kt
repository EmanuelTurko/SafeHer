package com.example.safeher.auth.safeCircle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.model.ContactItem

class ContactAdapter(
    private val maxSelection: Int = 5
) : ListAdapter<ContactItem, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    private val selectedItems = mutableListOf<ContactItem>()

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val name: TextView = view.findViewById(R.id.contactName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val item = getItem(position)
        holder.name.text = item.name
        holder.checkbox.isChecked = item.isSelected

        holder.checkbox.setOnClickListener {
            if (item.isSelected) {
                item.isSelected = false
                selectedItems.removeAll { it.phoneNumber == item.phoneNumber }
            } else {
                if (selectedItems.size >= maxSelection) {
                    Toast.makeText(
                        holder.itemView.context,
                        "You can only select up to $maxSelection contacts.",
                        Toast.LENGTH_SHORT
                    ).show()
                    holder.checkbox.isChecked = false
                    return@setOnClickListener
                }
                item.isSelected = true
                selectedItems.add(item)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = currentList.size

    fun getSelectedContacts(): List<ContactItem> = selectedItems

    fun setPreSelectedContacts(preSelected: List<ContactItem>) {
        selectedItems.clear()
        selectedItems.addAll(preSelected)

        val updatedList = currentList.map { contact ->
            contact.copy(isSelected = preSelected.any { it.phoneNumber == contact.phoneNumber })
        }
        submitList(updatedList)
    }
}

class ContactDiffCallback : DiffUtil.ItemCallback<ContactItem>() {
    override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
        return oldItem.phoneNumber == newItem.phoneNumber
    }

    override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
        return oldItem == newItem
    }
}
