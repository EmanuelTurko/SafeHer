package com.example.safeher.home_screen.sisters

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
import com.example.safeher.model.api.CreatePostRequest
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewPostFragment : Fragment() {

    private lateinit var editTextPostContent: EditText
    private lateinit var buttonSubmitPost: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_post, container, false)
        editTextPostContent = view.findViewById(R.id.editTextPostContent)
        buttonSubmitPost   = view.findViewById(R.id.buttonSubmitPost)

        buttonSubmitPost.setOnClickListener {
            val postText = editTextPostContent.text.toString().trim()
            if (postText.isNotEmpty()) {
                submitPost(postText)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please enter some text",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return view
    }

    private fun submitPost(text: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetroFitClient
                    .getApiService(requireContext())
                    .createPost(CreatePostRequest(body = text))

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            requireContext(),
                            "Post submitted!",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController()
                            .navigate(R.id.action_newPostFragment_to_sistersFragment)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to create post: ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}