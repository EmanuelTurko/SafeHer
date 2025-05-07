package com.example.safeher.auth.safeCircle.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.model.ContactItem

class ConfirmContactsAdapter(
    private val contacts: List<ContactItem>,
    private val onDeleteClick: (ContactItem) -> Unit
) : RecyclerView.Adapter<ConfirmContactsAdapter.ConfirmViewHolder>() {

    inner class ConfirmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTV: TextView = itemView.findViewById(R.id.nameTextView)
        private val phoneTV: TextView = itemView.findViewById(R.id.phoneTextView)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(contact: ContactItem) {
            nameTV.text = contact.name
            phoneTV.text = contact.phoneNumber
            deleteBtn.setOnClickListener {
                onDeleteClick(contact)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfirmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_confirm_contact, parent, false)
        return ConfirmViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConfirmViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount() = contacts.size
}
