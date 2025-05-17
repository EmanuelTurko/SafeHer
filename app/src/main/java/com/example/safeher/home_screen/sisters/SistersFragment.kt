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
        val writePostBtn = view.findViewById<MaterialButton>(R.id.write_new_post_button)
        writePostBtn.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_newPostFragment)
        }
    }

    private fun initListener() {
        mBackBtn.setOnClickListener {
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
        // 1. Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_post, null)

        // 2. Bind views
        val tvDialogPostAuthor = dialogView.findViewById<TextView>(R.id.tvDialogPostAuthor)
        val tvDialogPostTime   = dialogView.findViewById<TextView>(R.id.tvDialogPostTime)
        val tvBody             = dialogView.findViewById<TextView>(R.id.tvPostBody)
        val rvComments         = dialogView.findViewById<RecyclerView>(R.id.rvComments)
        val etNewComment       = dialogView.findViewById<EditText>(R.id.etNewComment)

        // 3. Populate post data
        tvDialogPostAuthor.text = post.user.fullName
        tvDialogPostTime.text   = DateUtils.formatDateTime(post.createdAt)
        tvBody.text             = post.body

        // 4. Use the original mutable list so updates persist
        val commentsList = post.comments
        val commentsAdapter = CommentsAdapter(requireContext(), commentsList)
        rvComments.layoutManager = LinearLayoutManager(requireContext())
        rvComments.adapter       = commentsAdapter

        // 5. Build and show the dialog
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

        // 6. Handle the Send button manually so dialog stays open
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text = etNewComment.text.toString().trim()
            if (text.isEmpty()) {
                etNewComment.error = "Write a comment"
                return@setOnClickListener
            }

            // 7. Send the new comment to the server
            lifecycleScope.launch(Dispatchers.IO) {
                val resp = RetroFitClient
                    .getApiService(requireContext())
                    .createComment(post.id, CommentRequest(text))

                withContext(Dispatchers.Main) {
                    if (resp.data != null) {
                        // 8. Add to the same list and notify adapter
                        commentsList.add(resp.data)
                        commentsAdapter.notifyItemInserted(commentsList.size - 1)
                        etNewComment.text.clear()
                        rvComments.scrollToPosition(commentsList.size - 1)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Error sending comment",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

}
