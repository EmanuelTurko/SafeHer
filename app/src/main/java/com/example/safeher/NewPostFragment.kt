package com.example.safeher

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import com.example.safeher.model.User
import com.example.safeher.utils.DateUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class NewPostFragment : Fragment() {

    private lateinit var editTextPostContent: EditText
    private lateinit var buttonSubmitPost: MaterialButton
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_post, container, false)
        editTextPostContent = view.findViewById(R.id.editTextPostContent)
        buttonSubmitPost   = view.findViewById(R.id.buttonSubmitPost)

        // Grab the stored user ID
        userId = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("id", "")!!

        buttonSubmitPost.setOnClickListener {
            val postText = editTextPostContent.text.toString().trim()
            if (postText.isNotEmpty()) {
                submitPost(postText)
            } else {
                Toast.makeText(requireContext(),
                    "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    private fun submitPost(text: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch the full User object so we can pass it into Post
                val profileResp = RetroFitClient
                    .getApiService(requireContext())
                    .getUserProfile(userId)
                val currentUser = profileResp.data
                    ?: throw IllegalStateException("Failed to load user")

                // 2. Build a Post with all required fields
                val newPost = Post(
                    id        = "",
                    body      = text,
                    user      = currentUser,
                    createdAt = Date().toInstant().toString(),
                    comments  = arrayListOf()
                )

                // 3. Send createPost
                RetroFitClient
                    .getApiService(requireContext())
                    .createPost(newPost)

                // 4. Back to Main thread for UI
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        "Post submitted!", Toast.LENGTH_SHORT).show()
                    findNavController()
                        .navigate(R.id.action_newPostFragment_to_sistersFragment)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
