package com.example.safeher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.model.api.NotificationItem

class NotificationAdapter(private val notifications: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationText: TextView = itemView.findViewById(R.id.notificationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        val message = when (notification.type) {
            "comment" -> "💬 ${notification.fromUser.fullName} commented on your post"
            "like" -> "❤️ ${notification.fromUser.fullName} liked your post"
            else -> "📢 You have a new notification"
        }

        holder.notificationText.text = message
    }


    override fun getItemCount(): Int = notifications.size
}

