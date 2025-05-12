package com.example.safeher.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
    private val showPostDialog: (Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView   = itemView.findViewById(R.id.tvPostAuthor)
        val tvTime: TextView     = itemView.findViewById(R.id.tvPostTime)
        val tvBody: TextView     = itemView.findViewById(R.id.textViewPost)
        val ivDelete: ImageView  = itemView.findViewById(R.id.buttonDelete)
        val ivComment: ImageView = itemView.findViewById(R.id.buttonComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.tvAuthor.text = post.user.fullName
        holder.tvTime.text   = DateUtils.formatDateTime(post.createdAt)
        holder.tvBody.text   = post.body

        holder.ivDelete.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val response: Response<Void> = RetroFitClient
                    .getApiService(context)
                    .deletePost(post.id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        posts.removeAt(position)
                        notifyItemRemoved(position)
                    } else {
                        // אפשר להציג Toast לשגיאה
                    }
                }
            }
        }

        holder.ivComment.setOnClickListener {
            showPostDialog(post)
        }
    }

    override fun getItemCount(): Int = posts.size
}
