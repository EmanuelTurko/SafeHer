package com.example.safeher.home_screen.sisters

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.safeher.adapters.CommentsAdapter
import com.example.safeher.adapters.PostAdapter
import com.example.safeher.api.RetroFitAiClient.api
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Comment
import com.example.safeher.model.Post
import com.example.safeher.model.api.CommentRequest
import com.example.safeher.utils.setupUI
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date


class SistersFragment :Fragment() {
    lateinit var mBackBtn: CardView
    lateinit var mHome: CardView
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
            findNavController().navigate(R.id.action_sistersFragment_to_newPostFragment)        }

    }


    private fun initListener() {
//        mBackBtn.setOnClickListener {
//            findNavController().navigate(R.id.action_sistersFragment_to_SOSHomeScreenFragment2)
//        }

        mHome.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_SOSHomeScreenFragment)
        }
    }
    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                val posts = RetroFitClient.getApiService(requireContext()).getAllPosts()
                postAdapter = PostAdapter(posts) { post ->
                    showPostDialog(post)
                }
                recyclerView.adapter = postAdapter
            } catch (e: Exception) {
                Log.e("SistersFragment", "שגיאה בטעינת פוסטים: ${e.message}")
            }
        }
    }

    private fun showPostDialog(post: Post) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_post, null)

        val tvBody = dialogView.findViewById<TextView>(R.id.tvPostBody)
        val rvComments = dialogView.findViewById<RecyclerView>(R.id.rvComments)
        val etNewComment = dialogView.findViewById<EditText>(R.id.etNewComment)

        tvBody.text = post.body

        rvComments.layoutManager = LinearLayoutManager(requireContext())
        val commentsAdapter = CommentsAdapter(post.comments.toMutableList())
        rvComments.adapter = commentsAdapter

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Send") { dialog, _ ->
                val text = etNewComment.text.toString().trim()
                if (text.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val resp = RetroFitClient.getApiService(requireContext()).createComment(post.id, CommentRequest(text))
                        withContext(Dispatchers.Main) {
                            if (resp.data != null) {
                                commentsAdapter.createComment(resp.data)
                                etNewComment.text.clear()
                                rvComments.scrollToPosition(commentsAdapter.itemCount - 1)
                            } else {
                                Toast.makeText(requireContext(),
                                    "שגיאה בשליחת תגובה", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    }