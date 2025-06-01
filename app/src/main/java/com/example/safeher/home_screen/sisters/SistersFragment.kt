package com.example.safeher.home_screen.sisters

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.adapters.CommentsAdapter
import com.example.safeher.adapters.PostAdapter
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import com.example.safeher.model.api.CommentRequest
import com.example.safeher.utils.DateUtils
import com.example.safeher.utils.setupUI
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.ImageButton

class SistersFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var backBtn: CardView
    private lateinit var horizontalRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var notificationsButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sisters, container, false)

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        MapsInitializer.initialize(requireContext())
        mapView.getMapAsync { googleMap ->
            googleMap.uiSettings.isCompassEnabled = true
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = false

            val israelBounds = LatLngBounds(
                LatLng(29.0, 34.0),
                LatLng(33.6, 35.9)
            )
            googleMap.setLatLngBoundsForCameraTarget(israelBounds)

            googleMap.setMinZoomPreference(7.0f)
            googleMap.setMaxZoomPreference(15.0f)

            val israelCenter = LatLng(31.0461, 34.8516)
            val cameraPosition = CameraPosition.Builder()
                .target(israelCenter)
                .zoom(10.0f)
                .build()
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }

        initView(view)
        initListener()
        setupHorizontalScroll(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().setupUI(view)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    private fun initView(view: View) {
        backBtn = view.findViewById(R.id.backButtonCard)
        val writePostBtn = view.findViewById<MaterialButton>(R.id.write_new_post_button)
        writePostBtn.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_newPostFragment)
        }

        notificationsButton = view.findViewById(R.id.notificationsButton)
        notificationsButton.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_notificationsFragment)
        }
    }


    private fun initListener() {
        backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_SOSHomeScreenFragment)
        }
    }

    private fun setupHorizontalScroll(view: View) {
        horizontalRecyclerView = view.findViewById(R.id.postsRecyclerView)
        horizontalRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

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
                        val action = SistersFragmentDirections
                            .actionSistersFragmentToEditPostFragment(post.id, post.body)
                        findNavController().navigate(action)
                    }
                )
                horizontalRecyclerView.adapter = postAdapter
            } catch (e: Exception) {
                Log.e("SistersFragment", "Error loading posts: ${e.message}")
            }
        }
    }

    private fun showPostDialog(post: Post) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_post, null)

        val tvAuthor = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogPostAuthor)
        val tvTime = dialogView.findViewById<android.widget.TextView>(R.id.tvDialogPostTime)
        val tvBody = dialogView.findViewById<android.widget.TextView>(R.id.tvPostBody)
        val rvComments = dialogView.findViewById<RecyclerView>(R.id.rvComments)
        val etNewComment = dialogView.findViewById<android.widget.EditText>(R.id.etNewComment)

        tvAuthor.text = post.user.fullName
        tvTime.text = DateUtils.formatDateTime(post.createdAt)
        tvBody.text = post.body

        val commentsList = post.comments.toMutableList()
        val commentsAdapter = CommentsAdapter(requireContext(), commentsList)
        rvComments.layoutManager = LinearLayoutManager(requireContext())
        rvComments.adapter = commentsAdapter

        val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("userId", "") ?: ""
        val isOwner = post.user.id == currentUserId

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Close", null)

        if (isOwner) {
            builder.setNeutralButton("Edit") { _, _ ->
                val action = SistersFragmentDirections
                    .actionSistersFragmentToEditPostFragment(post.id, post.body)
                findNavController().navigate(action)
            }
        }

        builder.setPositiveButton("Send", null)
        val dialog = builder.create().apply { show() }

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text = etNewComment.text.toString().trim()
            if (text.isEmpty()) {
                etNewComment.error = "Write a comment"
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val resp = RetroFitClient
                    .getApiService(requireContext())
                    .createComment(post.id, CommentRequest(text))

                withContext(Dispatchers.Main) {
                    if (resp.data != null) {
                        commentsList.add(resp.data)
                        commentsAdapter.notifyItemInserted(commentsList.size - 1)
                        etNewComment.text.clear()
                        rvComments.scrollToPosition(commentsList.size - 1)
                    } else {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Error sending comment",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
