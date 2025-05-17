package com.example.safeher.home_screen.sisters

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.adapters.CommentsAdapter
import com.example.safeher.adapters.PostAdapter
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import com.example.safeher.model.api.CommentRequest
import com.example.safeher.utils.DateUtils
import com.example.safeher.utils.setupUI
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SistersFragment : Fragment() {
    private lateinit var mBackBtn: CardView
    private lateinit var mHome: CardView
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sisters, container, false)
        initView(view)
        initListener()
        setupRecyclerView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().setupUI(view)
    }

    private fun initView(view: View) {
        mBackBtn = view.findViewById(R.id.backButtonCard)
        mHome = view.findViewById(R.id.homeButtonCard)
        val writePostBtn = view.findViewById<MaterialButton>(R.id.write_new_post_button)
        writePostBtn.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_newPostFragment)
        }
    }

    private fun initListener() {
        mHome.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_SOSHomeScreenFragment)
        }
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                val postsList: List<Post> = RetroFitClient
                    .getApiService(requireContext())
                    .getAllPosts()

                postAdapter = PostAdapter(
                    requireContext(),
                    postsList.toMutableList(),
                    showPostDialog = { post ->
                        showPostDialog(post)
                    },
                    onEditPost = { post ->
                        val action = SistersFragmentDirections
                            .actionSistersFragmentToEditPostFragment(post.id, post.body)
                        findNavController().navigate(action)
                    }
                )

                recyclerView.adapter = postAdapter
            } catch (e: Exception) {
                Log.e("SistersFragment", "Error loading posts: ${e.message}")
            }
        }
    }

    private fun showPostDialog(post: Post) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_post, null)

        val tvDialogPostAuthor = dialogView.findViewById<TextView>(R.id.tvDialogPostAuthor)
        val tvDialogPostTime   = dialogView.findViewById<TextView>(R.id.tvDialogPostTime)
        val tvBody             = dialogView.findViewById<TextView>(R.id.tvPostBody)
        val rvComments         = dialogView.findViewById<RecyclerView>(R.id.rvComments)
        val etNewComment       = dialogView.findViewById<EditText>(R.id.etNewComment)

        tvDialogPostAuthor.text = post.user?.fullName ?: "Anonymous"
        tvDialogPostTime.text   = DateUtils.formatDateTime(post.createdAt)
        tvBody.text             = post.body

        val commentsList    = post.comments.toMutableList()
        val commentsAdapter = CommentsAdapter(commentsList)
        rvComments.layoutManager = LinearLayoutManager(requireContext())
        rvComments.adapter        = commentsAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .setNeutralButton("Edit") { _, _ ->
                val action = SistersFragmentDirections
                    .actionSistersFragmentToEditPostFragment(post.id, post.body)
                findNavController().navigate(action)
            }
            .setPositiveButton("Send", null)
            .create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text = etNewComment.text.toString().trim()
            if (text.isEmpty()) {
                etNewComment.error = "Write a comment"
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val resp = RetroFitClient
                    .getApiService(requireContext())
                    .createComment(post.id, CommentRequest(text))

                withContext(Dispatchers.Main) {
                    resp.data?.let { newComment ->
                        commentsAdapter.addComment(newComment)
                        etNewComment.text.clear()
                        rvComments.scrollToPosition(commentsAdapter.itemCount - 1)
                    } ?: Toast.makeText(
                        requireContext(),
                        "Error sending comment",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
