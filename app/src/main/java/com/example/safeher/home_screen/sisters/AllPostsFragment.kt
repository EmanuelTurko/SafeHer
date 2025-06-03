package com.example.safeher.home_screen.sisters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.adapters.PostAdapter
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllPostsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_posts, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewAllPosts)
        setupRecyclerView()
        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            try {
                val postsList: List<Post> = RetroFitClient
                    .getApiService(requireContext())
                    .getAllPosts()

                postAdapter = PostAdapter(
                    context = requireContext(),
                    posts = postsList.toMutableList(),
                    showPostDialog = { post -> showPostDialog(post) },
                    onEditPost = { post ->
                        val action = AllPostsFragmentDirections
                            .actionAllPostsFragmentToEditPostFragment(
                                post.id,
                                post.body
                            )
                        findNavController().navigate(action)
                    },
                    isCarousel = false,
                    onShowAll = { /* not used in full mode */ },
                    onNewPostClick = {
                        // In FULL mode, you might not need “new post” tile, but just provide a no-op
                        // or navigate to NewPostFragment if desired:
                        findNavController().navigate(R.id.action_sistersFragment_to_newPostFragment)
                    }
                )
                recyclerView.adapter = postAdapter
            } catch (e: Exception) {
                Log.e("AllPostsFragment", "Error loading posts: ${e.message}")
            }
        }
    }

    private fun showPostDialog(post: Post) {
        // Copy the same dialog logic from SistersFragment (omitted here for brevity).
        // E.g. inflate R.layout.dialog_post and show comments, etc.
    }
}
