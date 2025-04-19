package com.example.safeher.settings.pair.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.model.ContactItem

class ContactAdapter(
    private val contacts: List<ContactItem>,
    private val maxSelection: Int = 5
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

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
        val item = contacts[position]
        holder.name.text = item.name
        holder.checkbox.isChecked = item.isSelected

        holder.checkbox.setOnClickListener {
            if (item.isSelected) {
                item.isSelected = false
                selectedItems.remove(item)
            } else {
                if (selectedItems.size >= maxSelection) {
                    Toast.makeText(holder.itemView.context, "You can only select up to $maxSelection contacts.", Toast.LENGTH_SHORT).show()
                    holder.checkbox.isChecked = false
                    return@setOnClickListener
                }
                item.isSelected = true
                selectedItems.add(item)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun getSelectedContacts(): List<ContactItem> = selectedItems
}
