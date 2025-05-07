package com.example.safeher.home_screen.sisters

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
import com.example.safeher.adapters.PostAdapter
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import com.example.safeher.utils.setupUI
import com.google.android.material.button.MaterialButton


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
                postAdapter = PostAdapter(posts)
                recyclerView.adapter = postAdapter
            } catch (e: Exception) {
                Log.e("SistersFragment", "שגיאה בטעינת פוסטים: ${e.message}")
            }
        }
    }

}