package com.example.safeher.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import com.example.safeher.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class PostAdapter(
    private val context: Context,
    private val posts: MutableList<Post>,
    private val showPostDialog: (Post) -> Unit,
    private val onEditPost: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView   = itemView.findViewById(R.id.tvPostAuthor)
        val tvTime: TextView     = itemView.findViewById(R.id.tvPostTime)
        val tvBody: TextView     = itemView.findViewById(R.id.textViewPost)
        val ivDelete: ImageView  = itemView.findViewById(R.id.buttonDelete)
        val ivComment: ImageView = itemView.findViewById(R.id.buttonComment)
        val ivEdit: ImageButton  = itemView.findViewById(R.id.buttonEdit)
        val ivLike: ImageView = itemView.findViewById(R.id.buttonLike)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        Log.d("PostAdapter", "post JSON = $post")

        holder.tvAuthor.text = post.user?.fullName ?: "Anonymous"
        holder.tvTime.text   = DateUtils.formatDateTime(post.createdAt)
        holder.tvBody.text   = post.body

        // Entire item click shows dialog
        holder.itemView.setOnClickListener {
            showPostDialog(post)
        }

        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("userId", "") ?: ""
        val isOwner = post.user.id == currentUserId

        holder.ivEdit.visibility   = if (isOwner) View.VISIBLE else View.GONE
        holder.ivDelete.visibility = if (isOwner) View.VISIBLE else View.GONE

        holder.ivDelete.setOnClickListener {
            if (!isOwner) return@setOnClickListener
            CoroutineScope(Dispatchers.IO).launch {
                val response: Response<Void> = RetroFitClient
                    .getApiService(context)
                    .deletePost(post.id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        posts.removeAt(position)
                        notifyItemRemoved(position)
                    } else {
                        Log.e("PostAdapter", "deletePost failed: ${response.code()} / ${response.errorBody()?.string()}")
                        Toast.makeText(
                            context,
                            "Failed to delete post: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        holder.ivComment.setOnClickListener {
            showPostDialog(post)
        }

        holder.ivEdit.setOnClickListener {
            if (isOwner) onEditPost(post)
        }

        holder.ivLike.setOnClickListener {
            Log.d("PostAdapter", "👆 נלחץ כפתור לייק") // שורת בדיקה
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetroFitClient.getApiService(context).likePost(post.id)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Log.d("PostAdapter", "✅ לייק הצליח")
                            Toast.makeText(context, "Liked!", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("PostAdapter", "❌ לייק נכשל: ${response.code()}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PostAdapter", "❌ שגיאה ב־likePost: ${e.localizedMessage}")
                }
            }
        }


    }

    override fun getItemCount(): Int = posts.size
}
