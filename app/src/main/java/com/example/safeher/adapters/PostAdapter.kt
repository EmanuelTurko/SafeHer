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
    private val onEditPost: (Post) -> Unit,
    private val isCarousel: Boolean,
    private val onShowAll: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CAROUSEL = 0
        private const val VIEW_TYPE_SHOW_ALL = 1
        private const val VIEW_TYPE_FULL     = 2
    }

    inner class PostCarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView       = itemView.findViewById(R.id.tvPostAuthor_carousel)
        val tvTime: TextView         = itemView.findViewById(R.id.tvPostTime_carousel)
        val tvBody: TextView         = itemView.findViewById(R.id.textViewPost_carousel)
        val btnLike: ImageButton     = itemView.findViewById(R.id.buttonLike_carousel)
        val btnComment: ImageButton  = itemView.findViewById(R.id.buttonComment_carousel)
    }

    inner class PostFullViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView       = itemView.findViewById(R.id.tvPostAuthor_full)
        val tvTime: TextView         = itemView.findViewById(R.id.tvPostTime_full)
        val tvBody: TextView         = itemView.findViewById(R.id.textViewPost_full)
        val btnLike: ImageButton     = itemView.findViewById(R.id.buttonLike_full)
        val btnComment: ImageButton  = itemView.findViewById(R.id.buttonComment_full)
        val btnEdit: ImageButton     = itemView.findViewById(R.id.buttonEdit_full)
        val btnDelete: ImageButton   = itemView.findViewById(R.id.buttonDelete_full)
    }

    inner class ShowAllViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    }

    override fun getItemCount(): Int {
        return if (isCarousel) {
            posts.size + 1
        } else {
            posts.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isCarousel) {
            if (position == posts.size) {
                VIEW_TYPE_SHOW_ALL
            } else {
                VIEW_TYPE_CAROUSEL
            }
        } else {
            VIEW_TYPE_FULL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CAROUSEL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_post_carousel, parent, false)
                PostCarouselViewHolder(view)
            }
            VIEW_TYPE_SHOW_ALL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_show_all, parent, false)
                ShowAllViewHolder(view)
            }
            else /* VIEW_TYPE_FULL */ -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_post_full, parent, false)
                PostFullViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ShowAllViewHolder) {
            holder.itemView.setOnClickListener {
                onShowAll.invoke()
            }
            return
        }

        if (holder is PostCarouselViewHolder) {
            val post = posts[position]
            holder.tvAuthor.text = post.user.fullName ?: "Anonymous"
            holder.tvTime.text   = DateUtils.formatDateTime(post.createdAt)
            holder.tvBody.text   = post.body

            holder.itemView.setOnClickListener { showPostDialog(post) }
            holder.btnLike.setOnClickListener {
                Toast.makeText(context, "Liked!", Toast.LENGTH_SHORT).show()
            }
            holder.btnComment.setOnClickListener {
                showPostDialog(post)
            }
            return
        }

        if (holder is PostFullViewHolder) {
            val post = posts[position]
            val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            val currentUserId = prefs.getString("userId", "") ?: ""
            val isOwner = post.user.id == currentUserId

            holder.tvAuthor.text = post.user.fullName ?: "Anonymous"
            holder.tvTime.text   = DateUtils.formatDateTime(post.createdAt)
            holder.tvBody.text   = post.body

            holder.itemView.setOnClickListener { showPostDialog(post) }
            holder.btnLike.setOnClickListener {
                Toast.makeText(context, "Liked!", Toast.LENGTH_SHORT).show()
            }
            holder.btnComment.setOnClickListener {
                showPostDialog(post)
            }

            holder.btnEdit.visibility   = if (isOwner) View.VISIBLE else View.GONE
            holder.btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE

            holder.btnEdit.setOnClickListener {
                if (isOwner) onEditPost(post)
            }
            holder.btnDelete.setOnClickListener {
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
                            Toast.makeText(
                                context,
                                "Failed to delete post: ${response.code()}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
