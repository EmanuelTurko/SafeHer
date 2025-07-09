package com.example.safeher.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Comment
import com.example.safeher.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class CommentsAdapter(
    private val context: Context,
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAuthor: TextView     = view.findViewById(R.id.tvCommentAuthor)
        val tvTime:   TextView     = view.findViewById(R.id.tvCommentTime)
        val tvBody:   TextView     = view.findViewById(R.id.tvCommentBody)
        val btnDelete: ImageButton = view.findViewById(R.id.buttonDeleteComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val c = comments[position]

        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("userId", "") ?: ""

        val isAuthor = c.user.id == currentUserId

        holder.tvAuthor.text = c.user.fullName
        holder.tvTime.text   = DateUtils.formatDateTime(c.createdAt)
        holder.tvBody.text   = c.body

        holder.btnDelete.visibility = if (isAuthor) View.VISIBLE else View.GONE

        holder.btnDelete.setOnClickListener {
            if (!isAuthor) return@setOnClickListener

            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            CoroutineScope(Dispatchers.IO).launch {
                val response: Response<Void> = RetroFitClient
                    .getApiService(context)
                    .deleteComment(comments[pos].id)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        comments.removeAt(pos)
                        notifyItemRemoved(pos)
                    } else {
                        Toast.makeText(
                            context,
                            "Failed to delete comment: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = comments.size

    fun addComment(comment: Comment) {
        comments.add(comment)
        notifyItemInserted(comments.size - 1)
    }
}
