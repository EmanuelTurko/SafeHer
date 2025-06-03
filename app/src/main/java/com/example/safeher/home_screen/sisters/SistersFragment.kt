package com.example.safeher.home_screen.sisters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.request.target.Target
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
import com.google.android.gms.maps.model.BitmapDescriptor
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

            // Define bounds roughly covering Israel initially (fallback)
            val israelBounds = LatLngBounds(
                LatLng(29.0, 34.0),   // southwest corner
                LatLng(33.6, 35.9)    // northeast corner
            )
            googleMap.setLatLngBoundsForCameraTarget(israelBounds)
            googleMap.setMinZoomPreference(7.0f)
            googleMap.setMaxZoomPreference(15.0f)
            val israelCenter = LatLng(31.0461, 34.8516)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(israelCenter, 10.0f))

            // Fetch all users, geocode their "city", add custom markers, then fit camera
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
                        val action = SistersFragmentDirections
                            .actionSistersFragmentToAllPostsFragment()
                        findNavController().navigate(action)
                    },
                    onNewPostClick = {
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

                Log.d("SistersFragment", "Fetched ${users.size} users from API")

                if (users.isEmpty()) {
                    Log.d("SistersFragment", "No users to display.")
                    return@launch
                }

                // 2) Build LatLngBounds to include all markers
                val boundsBuilder = LatLngBounds.Builder()

                // נעביר את הלולאה ל־IO כדי לקרוא ל־Geocoder
                withContext(Dispatchers.IO) {
                    for (user in users) {
                        val cityName = user.city ?: ""
                        if (cityName.isBlank()) {
                            Log.d("SistersFragment", "Skipping user ${user.fullName} because city is blank")
                            continue
                        }

                        try {
                            Log.d("SistersFragment", "Geocoding cityName=\"$cityName\" for user=${user.fullName}")
                            val geocoder = Geocoder(requireContext(), Locale.getDefault())
                            val addressList: List<android.location.Address>? = geocoder.getFromLocationName(cityName, 1)

                            if (addressList != null && addressList.isNotEmpty()) {
                                val address = addressList[0]
                                val userLatLng = LatLng(address.latitude, address.longitude)
                                Log.d("SistersFragment", "User ${user.fullName} geocoded to lat=${address.latitude}, lon=${address.longitude}")

                                // נוסיף את הקואורדינטה לבנאי הגבולות
                                withContext(Dispatchers.Main) {
                                    boundsBuilder.include(userLatLng)
                                }

                                // 3) בחזרה ל־Main: טוענים את תמונת הפרופיל וצובעים את הסימן
                                withContext(Dispatchers.Main) {
                                    val isHelper = user.safeCircleContacts?.isNotEmpty() == true
                                    Log.d("SistersFragment", "Preparing to create marker icon for ${user.fullName}. isHelper=$isHelper")

                                    createCustomMarkerIcon(user, isHelper) { descriptor ->
                                        if (descriptor != null) {
                                            Log.d("SistersFragment", "Adding marker for ${user.fullName} at $userLatLng")
                                            googleMap.addMarker(
                                                MarkerOptions()
                                                    .position(userLatLng)
                                                    .title(user.fullName)
                                                    .icon(descriptor)
                                            )
                                        } else {
                                            Log.e("SistersFragment", "Descriptor was null for ${user.fullName}, adding default marker")
                                            googleMap.addMarker(
                                                MarkerOptions()
                                                    .position(userLatLng)
                                                    .title(user.fullName)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Log.e(
                                    "SistersFragment",
                                    "Geocoder returned empty or null for city: \"$cityName\" (user=${user.fullName})"
                                )
                            }
                        } catch (ge: Exception) {
                            Log.e("SistersFragment", "Geocoder exception for ${user.fullName}: ${ge.message}")
                        }
                    }
                }

                // 4) אחרי שציירנו את כל ה־Markers, נעביר את המצלמה לכלול את כולם
                withContext(Dispatchers.Main) {
                    try {
                        val bounds = boundsBuilder.build()
                        val width = resources.displayMetrics.widthPixels
                        val height = resources.displayMetrics.heightPixels
                        val padding = (0.20 * minOf(width, height)).toInt()
                        Log.d("SistersFragment", "Animating camera to bounds with padding=$padding")
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds, padding)
                        )
                    } catch (e: Exception) {
                        Log.e("SistersFragment", "Error animating camera to bounds: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e("SistersFragment", "Exception in loadAllUsersAndAddMarkers: ${e.message}")
            }
        }
    }

    /**
     * Asynchronously loads (או ברירת מחדל) את תמונת הפרופיל,
     * יוצר את ה־View המותאם למרקר, ומחזיר BitmapDescriptor ב־callback.
     */
    private fun createCustomMarkerIcon(
        user: User,
        isHelper: Boolean,
        callback: (BitmapDescriptor?) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            var profileBitmap: Bitmap? = null
            try {
                val url = user.profilePicture.orEmpty()
                if (url.isNotBlank()) {
                    Log.d("SistersFragment", "Loading profile image for ${user.fullName} from URL=\"$url\"")
                    // נטען את התמונה כ־Bitmap בגודל 100×100
                    val futureTarget: FutureTarget<Bitmap> = Glide.with(requireContext())
                        .asBitmap()
                        .load(url)
                        .circleCrop()
                        .submit(100, 100)

                    profileBitmap = futureTarget.get()
                    Glide.with(requireContext()).clear(futureTarget)
                    Log.d("SistersFragment", "Successfully loaded profile image for ${user.fullName}")
                } else {
                    Log.d("SistersFragment", "No profile URL for ${user.fullName}, using default.")
                }
            } catch (e: Exception) {
                Log.e("SistersFragment", "Failed loading profile image for ${user.fullName}: ${e.message}")
                profileBitmap = null
            }

            // חוזרים לחוט ה־Main בשביל לצייר את ה־View
            withContext(Dispatchers.Main) {
                try {
                    val markerView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.marker_user, null)

                    // 1) קביעת תמונת הפרופיל (או ברירת מחדל)
                    val ivProfile = markerView.findViewById<ImageView>(R.id.profileImageView)
                    if (profileBitmap != null) {
                        ivProfile.setImageBitmap(profileBitmap)
                    } else {
                        ivProfile.setImageResource(R.drawable.profile)
                    }

                    // 2) קביעת ה־statusDot (ירוק/אפור)
                    val statusDot = markerView.findViewById<View>(R.id.statusDot)
                    if (isHelper) {
                        statusDot.setBackgroundResource(R.drawable.circle_green)
                    } else {
                        statusDot.setBackgroundResource(R.drawable.circle_gray)
                    }

                    // 3) קביעת המחט בתחתית
                    val ivPointer = markerView.findViewById<ImageView>(R.id.pinPointer)
                    ivPointer.setImageResource(R.drawable.ic_map_pin)

                    // מודדים ומניחים את ה־View
                    markerView.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    )
                    markerView.layout(0, 0, markerView.measuredWidth, markerView.measuredHeight)

                    // יוצרים Bitmap בגודל ה־View
                    val bitmap = Bitmap.createBitmap(
                        markerView.measuredWidth,
                        markerView.measuredHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    markerView.draw(canvas)

                    Log.d("SistersFragment", "Created custom marker bitmap for ${user.fullName} (w=${bitmap.width}, h=${bitmap.height})")
                    callback(BitmapDescriptorFactory.fromBitmap(bitmap))
                } catch (e: Exception) {
                    Log.e("SistersFragment", "Error creating marker view for ${user.fullName}: ${e.message}")
                    callback(null)
                }
            }
        }
    }
}
