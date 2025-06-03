package com.example.safeher.home_screen.sisters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.adapters.PostAdapter
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.fragment.findNavController

class AllPostsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_posts, container, false).also { view ->
            recyclerView = view.findViewById(R.id.recyclerViewAllPosts)
            setupRecyclerView()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        lifecycleScope.launch {
            try {
                val postsList: List<Post> = RetroFitClient
                    .getApiService(requireContext())
                    .getAllPosts()

                postAdapter = PostAdapter(
                    requireContext(),
                    postsList.toMutableList(),
                    showPostDialog = { post -> showPostDialog(post) },
                    onEditPost = { post ->
                        val action = AllPostsFragmentDirections
                            .actionAllPostsFragmentToEditPostFragment(
                                post.id,
                                post.body
                            )
                        findNavController().navigate(action)
                    },
                    isCarousel = false,      // <<< עמוד מלא
                    onShowAll = { /* לא נחוץ כאן, כי אין לנו “Show All” בעמוד המלא */ }
                )
                recyclerView.adapter = postAdapter
            } catch (e: Exception) {
                Log.e("AllPostsFragment", "Error loading posts: ${e.message}")
            }
        }
    }

    private fun showPostDialog(post: Post) {
        // התוכן של ה־Dialog כמו ב־SistersFragment
    }
}



