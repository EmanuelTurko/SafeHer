package com.example.safeher.home_screen.sisters

import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.safeher.R
import com.example.safeher.adapters.CommentsAdapter
import com.example.safeher.adapters.NotificationAdapter
import com.example.safeher.adapters.PostAdapter
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import com.example.safeher.model.User
import com.example.safeher.model.api.CommentRequest
import com.example.safeher.utils.DateUtils
import com.example.safeher.utils.setupUI
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SistersFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var backBtn: CardView
    private lateinit var horizontalRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var notificationsButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout containing MapView as background
        val view = inflater.inflate(R.layout.fragment_sisters, container, false)

        // Initialize and manage MapView lifecycle
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        MapsInitializer.initialize(requireContext())
        mapView.getMapAsync { googleMap ->
            // Enable map UI controls
            googleMap.uiSettings.isCompassEnabled = true
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = false

            // Define bounds roughly covering Israel
            val israelBounds = LatLngBounds(
                LatLng(29.0, 34.0),   // southwest corner
                LatLng(33.6, 35.9)    // northeast corner
            )
            googleMap.setLatLngBoundsForCameraTarget(israelBounds)

            // Restrict minimum/maximum zoom levels
            googleMap.setMinZoomPreference(7.0f)
            googleMap.setMaxZoomPreference(15.0f)

            // Center camera on Israel with a higher initial zoom (cities more visible)
            val israelCenter = LatLng(31.0461, 34.8516)
            val cameraPosition = CameraPosition.Builder()
                .target(israelCenter)
                .zoom(10.0f) // zoom in further (10.0) for city-level view
                .build()
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

            // Now fetch all users, geocode their "city", and add a marker with their profile picture and name
            loadAllUsersAndAddMarkers(googleMap)
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

    // Manage MapView lifecycle
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        checkForNewNotifications()
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

        // Notification (bell) button hides the badge on click
        notificationsButton = view.findViewById(R.id.notificationsButton)
        notificationsButton.setOnClickListener {
            view.findViewById<View>(R.id.notificationBadge)?.visibility = View.GONE
            showNotificationDialog()
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
                    context = requireContext(),
                    posts = postsList.toMutableList(),
                    showPostDialog = { post -> showPostDialog(post) },
                    onEditPost = { post ->
                        val action = SistersFragmentDirections
                            .actionSistersFragmentToEditPostFragment(
                                post.id,
                                post.body
                            )
                        findNavController().navigate(action)
                    },
                    isCarousel = true,
                    onShowAll = {
                        // Navigate to AllPosts screen
                        val action = SistersFragmentDirections
                            .actionSistersFragmentToAllPostsFragment()
                        findNavController().navigate(action)
                    },
                    onNewPostClick = {
                        // Navigate to Create New Post screen
                        findNavController().navigate(R.id.action_sistersFragment_to_newPostFragment)
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

        val tvAuthor = dialogView.findViewById<TextView>(R.id.tvDialogPostAuthor)
        val tvTime = dialogView.findViewById<TextView>(R.id.tvDialogPostTime)
        val tvBody = dialogView.findViewById<TextView>(R.id.tvPostBody)
        val rvComments = dialogView.findViewById<RecyclerView>(R.id.rvComments)
        val etNewComment = dialogView.findViewById<EditText>(R.id.etNewComment)

        tvAuthor.text = post.user.fullName
        tvTime.text = DateUtils.formatDateTime(post.createdAt)
        tvBody.text = post.body

        val commentsList = post.comments.toMutableList()
        val commentsAdapter = CommentsAdapter(requireContext(), commentsList)
        rvComments.layoutManager = LinearLayoutManager(requireContext())
        rvComments.adapter = commentsAdapter

        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val currentUserId = prefs.getString("userId", "") ?: ""
        val isOwner = post.user.id == currentUserId

        val builder = AlertDialog.Builder(requireContext())
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
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
                        etNewComment.text?.clear()
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

    private fun showNotificationDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.notification_dialog, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.notificationRecyclerView)

        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = "Bearer " + (prefs.getString("token", "") ?: "")

        lifecycleScope.launch {
            try {
                val response = RetroFitClient
                    .getApiService(requireContext())
                    .getUserNotifications(token)

                if (response.isSuccessful) {
                    val notifications = response.body()?.data ?: emptyList()
                    recyclerView.layoutManager = LinearLayoutManager(requireContext())
                    recyclerView.adapter = NotificationAdapter(notifications)
                } else {
                    Log.e("Notifications", "Response not successful: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("Notifications", "Error fetching notifications: ${e.message}")
            }
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun checkForNewNotifications() {
        val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", "") ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetroFitClient
                    .getApiService(requireContext())
                    .hasUnreadNotifications(userId)

                val badge = view?.findViewById<View>(R.id.notificationBadge)
                if (response.isSuccessful) {
                    val hasUnread = response.body()?.data == true
                    badge?.visibility = if (hasUnread) View.VISIBLE else View.GONE
                    Log.d("BadgeCheck", "hasUnread=$hasUnread")
                } else {
                    Log.e("BadgeCheck", "❌ failed: ${response.code()}")
                    badge?.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("BadgeCheck", "❌ exception: ${e.localizedMessage}")
            }
        }
    }

    private fun loadAllUsersAndAddMarkers(googleMap: GoogleMap) {
        lifecycleScope.launch {
            try {
                // 1) Fetch all users from backend
                val users: List<User> = RetroFitClient
                    .getApiService(requireContext())
                    .getAllUsers()

                // 2) For each user, geocode their city and add marker
                withContext(Dispatchers.IO) {
                    for (user in users) {
                        val cityName = user.city ?: continue
                        if (cityName.isBlank()) continue

                        try {
                            Log.d("SistersFragment", "Geocoding cityName: \"$cityName\" for user ${user.fullName}")
                            val geocoder = Geocoder(requireContext(), Locale.getDefault())
                            val addressList = geocoder.getFromLocationName(cityName, 1)
                            Log.d("SistersFragment", "Geocoder returned: $addressList for user ${user.fullName}")

                            if (!addressList.isNullOrEmpty()) {
                                val address = addressList[0]
                                val userLatLng = LatLng(address.latitude, address.longitude)

                                withContext(Dispatchers.Main) {
                                    // 3) On Main: load profile pic with Glide if available, else use default marker
                                    val profilePicUrl = user.profilePicture ?: ""
                                    val fullName = user.fullName

                                    if (profilePicUrl.isBlank()) {
                                        // No profile picture => add default marker with name
                                        googleMap.addMarker(
                                            MarkerOptions()
                                                .position(userLatLng)
                                                .title(fullName)
                                        )
                                    } else {
                                        // Load profile picture and place marker
                                        Glide.with(requireContext())
                                            .asBitmap()
                                            .load(profilePicUrl)
                                            .circleCrop()
                                            .into(object : CustomTarget<Bitmap>(100, 100) {
                                                override fun onResourceReady(
                                                    resource: Bitmap,
                                                    transition: Transition<in Bitmap>?
                                                ) {
                                                    val markerIcon = BitmapDescriptorFactory.fromBitmap(resource)
                                                    googleMap.addMarker(
                                                        MarkerOptions()
                                                            .position(userLatLng)
                                                            .title(fullName)
                                                            .icon(markerIcon)
                                                    )
                                                }

                                                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                                                    // no-op
                                                }

                                                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                                    super.onLoadFailed(errorDrawable)
                                                    // On failure, fallback to default marker
                                                    googleMap.addMarker(
                                                        MarkerOptions()
                                                            .position(userLatLng)
                                                            .title(fullName)
                                                    )
                                                }
                                            })
                                    }
                                }
                            } else {
                                Log.e(
                                    "SistersFragment",
                                    "Geocoder returned empty for city: \"$cityName\""
                                )
                            }
                        } catch (ge: Exception) {
                            Log.e("SistersFragment", "Geocoder exception for user ${user.fullName}: ${ge.message}")
                        }
                    }
                    // 4) After adding all markers, optionally adjust camera to show all
                    // (not required; the camera is currently centered on Israel)
                }
            } catch (e: Exception) {
                Log.e("SistersFragment", "Exception in loadAllUsersAndAddMarkers: ${e.message}")
            }
        }
    }
}
