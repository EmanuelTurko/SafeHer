package com.example.safeher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.model.Comment

class CommentsAdapter(
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.tvComment.text = comments[position].body
    }

    override fun getItemCount(): Int = comments.size

    fun createComment(comment: Comment) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvComment: TextView = view.findViewById(android.R.id.text1)
    }
}
