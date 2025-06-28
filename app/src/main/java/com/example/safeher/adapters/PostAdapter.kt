package com.example.safeher.adapters

import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
    private val onShowAll: () -> Unit,
    private val onNewPostClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_NEW_POST = 0
        private const val VIEW_TYPE_CAROUSEL = 1
        private const val VIEW_TYPE_SHOW_ALL = 2
        private const val VIEW_TYPE_FULL = 3
    }

    override fun getItemCount(): Int {
        return if (isCarousel) {
            posts.size + 2
        } else {
            posts.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isCarousel) {
            when (position) {
                0 -> VIEW_TYPE_NEW_POST
                posts.size + 1 -> VIEW_TYPE_SHOW_ALL
                else -> VIEW_TYPE_CAROUSEL
            }
        } else {
            VIEW_TYPE_FULL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NEW_POST -> {
                val view = inflater.inflate(R.layout.item_new_post, parent, false)
                NewPostViewHolder(view, onNewPostClick)
            }
            VIEW_TYPE_CAROUSEL -> {
                val view = inflater.inflate(R.layout.item_post_carousel, parent, false)
                PostCarouselViewHolder(view)
            }
            VIEW_TYPE_SHOW_ALL -> {
                val view = inflater.inflate(R.layout.item_show_all, parent, false)
                ShowAllViewHolder(view, onShowAll)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_post_full, parent, false)
                PostFullViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NewPostViewHolder -> {
                // No binding needed
            }
            is ShowAllViewHolder -> {
                // No binding needed
            }
            is PostCarouselViewHolder -> {
                val post = posts[position - 1]

                holder.tvAuthor.text = post.user?.fullName ?: "Anonymous"
                holder.tvTime.text = DateUtils.formatDateTime(post.createdAt)
                holder.tvBody.text = post.body

                holder.tvLikeCount.text = post.likeCount.toString()
                holder.tvCommentCount.text = post.commentCount.toString()

                val initialColor = if (post.isLiked) R.color.red_background else R.color.gray
                holder.btnLike.setColorFilter(
                    ContextCompat.getColor(context, initialColor),
                    PorterDuff.Mode.SRC_IN
                )

                holder.itemView.setOnClickListener {
                    showPostDialog(post)
                }

                holder.btnLike.setOnClickListener {
                    if (post.isLiked) {
                        post.isLiked = false
                        post.likeCount -= 1
                        holder.btnLike.setColorFilter(
                            ContextCompat.getColor(context, R.color.gray),
                            PorterDuff.Mode.SRC_IN
                        )
                    } else {
                        post.isLiked = true
                        post.likeCount += 1
                        holder.btnLike.setColorFilter(
                            ContextCompat.getColor(context, R.color.red_background),
                            PorterDuff.Mode.SRC_IN
                        )
                    }
                    holder.tvLikeCount.text = post.likeCount.toString()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response: Response<Post> =
                                RetroFitClient.getApiService(context).likePost(post.id)
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    val updatedPost = response.body()
                                    if (updatedPost != null) {
                                        post.likeCount = updatedPost.likeCount
                                        post.isLiked = updatedPost.isLiked
                                        holder.tvLikeCount.text = post.likeCount.toString()
                                        val newColor =
                                            if (post.isLiked) R.color.red_background else R.color.gray
                                        holder.btnLike.setColorFilter(
                                            ContextCompat.getColor(context, newColor),
                                            PorterDuff.Mode.SRC_IN
                                        )
                                    }
                                } else {
                                    Log.e("PostAdapter", "Like API failed: ${response.code()}")
                                    Toast.makeText(context, "שגיאה בלייק", Toast.LENGTH_SHORT).show()
                                    post.isLiked = !post.isLiked
                                    post.likeCount += if (post.isLiked) 1 else -1
                                    holder.tvLikeCount.text = post.likeCount.toString()
                                    val revertColor =
                                        if (post.isLiked) R.color.red_background else R.color.gray
                                    holder.btnLike.setColorFilter(
                                        ContextCompat.getColor(context, revertColor),
                                        PorterDuff.Mode.SRC_IN
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Log.e("PostAdapter", "Error in likePost: ${e.localizedMessage}")
                                Toast.makeText(context, "שגיאת רשת", Toast.LENGTH_SHORT).show()
                                post.isLiked = !post.isLiked
                                post.likeCount += if (post.isLiked) 1 else -1
                                holder.tvLikeCount.text = post.likeCount.toString()
                                val revertColor =
                                    if (post.isLiked) R.color.red_background else R.color.gray
                                holder.btnLike.setColorFilter(
                                    ContextCompat.getColor(context, revertColor),
                                    PorterDuff.Mode.SRC_IN
                                )
                            }
                        }
                    }
                }

                holder.btnComment.setOnClickListener {
                    showPostDialog(post)
                }
            }
            is PostFullViewHolder -> {
                val index = if (isCarousel) position - 1 else position
                val post = posts[index]

                val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                val currentUserId = prefs.getString("userId", "") ?: ""
                val authorName = post.user?.fullName ?: "Anonymous"
                val isOwner = post.user?.id == currentUserId

                holder.tvAuthor.text = authorName
                holder.tvTime.text = DateUtils.formatDateTime(post.createdAt)
                holder.tvBody.text = post.body

                holder.tvLikeCount.text = post.likeCount.toString()
                holder.tvCommentCount.text = post.commentCount.toString()

                val initialColor = if (post.isLiked) R.color.red_background else R.color.gray
                holder.btnLike.setColorFilter(
                    ContextCompat.getColor(context, initialColor),
                    PorterDuff.Mode.SRC_IN
                )

                holder.itemView.setOnClickListener {
                    showPostDialog(post)
                }

                holder.btnLike.setOnClickListener {
                    if (post.isLiked) {
                        post.isLiked = false
                        post.likeCount -= 1
                        holder.btnLike.setColorFilter(
                            ContextCompat.getColor(context, R.color.gray),
                            PorterDuff.Mode.SRC_IN
                        )
                    } else {
                        post.isLiked = true
                        post.likeCount += 1
                        holder.btnLike.setColorFilter(
                            ContextCompat.getColor(context, R.color.red_background),
                            PorterDuff.Mode.SRC_IN
                        )
                    }
                    holder.tvLikeCount.text = post.likeCount.toString()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response: Response<Post> =
                                RetroFitClient.getApiService(context).likePost(post.id)
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    val updatedPost = response.body()
                                    if (updatedPost != null) {
                                        post.likeCount = updatedPost.likeCount
                                        post.isLiked = updatedPost.isLiked
                                        holder.tvLikeCount.text = post.likeCount.toString()
                                        val newColor =
                                            if (post.isLiked) R.color.red_background else R.color.gray
                                        holder.btnLike.setColorFilter(
                                            ContextCompat.getColor(context, newColor),
                                            PorterDuff.Mode.SRC_IN
                                        )
                                    }
                                } else {
                                    Log.e("PostAdapter", "Like API failed: ${response.code()}")
                                    Toast.makeText(context, "שגיאה בלייק", Toast.LENGTH_SHORT).show()
                                    post.isLiked = !post.isLiked
                                    post.likeCount += if (post.isLiked) 1 else -1
                                    holder.tvLikeCount.text = post.likeCount.toString()
                                    val revertColor =
                                        if (post.isLiked) R.color.red_background else R.color.gray
                                    holder.btnLike.setColorFilter(
                                        ContextCompat.getColor(context, revertColor),
                                        PorterDuff.Mode.SRC_IN
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Log.e("PostAdapter", "Error in likePost: ${e.localizedMessage}")
                                Toast.makeText(context, "שגיאת רשת", Toast.LENGTH_SHORT).show()
                                post.isLiked = !post.isLiked
                                post.likeCount += if (post.isLiked) 1 else -1
                                holder.tvLikeCount.text = post.likeCount.toString()
                                val revertColor =
                                    if (post.isLiked) R.color.red_background else R.color.gray
                                holder.btnLike.setColorFilter(
                                    ContextCompat.getColor(context, revertColor),
                                    PorterDuff.Mode.SRC_IN
                                )
                            }
                        }
                    }
                }

                holder.btnComment.setOnClickListener {
                    showPostDialog(post)
                }

                holder.btnEdit.visibility = if (isOwner) View.VISIBLE else View.GONE
                holder.btnDelete.visibility = if (isOwner) View.VISIBLE else View.GONE

                holder.btnEdit.setOnClickListener {
                    if (isOwner) onEditPost(post)
                }
                holder.btnDelete.setOnClickListener {
                    if (!isOwner) return@setOnClickListener
                    CoroutineScope(Dispatchers.IO).launch {
                        val response: Response<Void> =
                            RetroFitClient.getApiService(context).deletePost(post.id)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                posts.removeAt(index)
                                val removePosition = if (isCarousel) index + 1 else index
                                notifyItemRemoved(removePosition)
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

    inner class NewPostViewHolder(
        itemView: View,
        onNewPostClick: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                onNewPostClick()
            }
        }
    }

    inner class PostCarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView = itemView.findViewById(R.id.tvPostAuthor_carousel)
        val tvTime: TextView = itemView.findViewById(R.id.tvPostTime_carousel)
        val tvBody: TextView = itemView.findViewById(R.id.textViewPost_carousel)
        val btnLike: ImageButton = itemView.findViewById(R.id.buttonLike_carousel)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount_carousel)
        val btnComment: ImageButton = itemView.findViewById(R.id.buttonComment_carousel)
        val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount_carousel)
    }

    inner class ShowAllViewHolder(
        itemView: View,
        onShowAllClick: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                onShowAllClick()
            }
        }
    }

    inner class PostFullViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAuthor: TextView = itemView.findViewById(R.id.tvPostAuthor_full)
        val tvTime: TextView = itemView.findViewById(R.id.tvPostTime_full)
        val tvBody: TextView = itemView.findViewById(R.id.textViewPost_full)
        val btnLike: ImageButton = itemView.findViewById(R.id.buttonLike_full)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount_full)
        val btnComment: ImageButton = itemView.findViewById(R.id.buttonComment_full)
        val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount_full)
        val btnEdit: ImageButton = itemView.findViewById(R.id.buttonEdit_full)
        val btnDelete: ImageButton = itemView.findViewById(R.id.buttonDelete_full)
    }
}
