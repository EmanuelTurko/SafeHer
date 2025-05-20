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
import com.example.safeher.utils.getStringListShareRef
import android.content.Context

class ContactAdapter(
    private val maxSelection: Int = 5,
    private val context: Context
) : ListAdapter<ContactItem, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

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

        // מסמן אנשי קשר שהיו שמורים בעבר (מ־SharedPreferences)
        val savedNumbers = context.getStringListShareRef("safeCircle", "or")
        if (savedNumbers.contains(item.phoneNumber)) {
            item.isSelected = true
        }

        holder.checkbox.isChecked = item.isSelected

        holder.checkbox.setOnClickListener {
            item.isSelected = !item.isSelected

            if (item.isSelected && getSelectedContacts().size > maxSelection) {
                item.isSelected = false
                holder.checkbox.isChecked = false
                Toast.makeText(
                    context,
                    "You can only select up to $maxSelection contacts.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = currentList.size

    // מחזיר את כל אנשי הקשר שסומנו בפועל
    fun getSelectedContacts(): List<ContactItem> {
        return currentList.filter { it.isSelected }
    }

    // מסמן אנשי קשר שהיו נבחרים במסך קודם
    fun setPreSelectedContacts(preSelected: List<ContactItem>) {
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
