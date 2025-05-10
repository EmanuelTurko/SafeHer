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
import com.example.safeher.api.RetroFitClient
import com.example.safeher.model.Post
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class NewPostFragment : Fragment() {

    private lateinit var editTextPostContent: EditText
    private lateinit var buttonSubmitPost: MaterialButton
    private lateinit var id : String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_post, container, false)
        editTextPostContent = view.findViewById(R.id.editTextPostContent)
        buttonSubmitPost   = view.findViewById(R.id.buttonSubmitPost)
        val idPrefs = context?.getSharedPreferences("auth", Context.MODE_PRIVATE)
        id = idPrefs?.getString("id", null) ?: ""

        buttonSubmitPost.setOnClickListener {
            val postText = editTextPostContent.text.toString().trim()
            if (postText.isNotEmpty()) {
                submitPost(postText)
            } else {
                Toast.makeText(requireContext(), "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun submitPost(text: String) {
        val newPost = Post(
            id = id,
            body = text,
//            user = TODO(),
//            likes = TODO(),
//            comments = TODO()
        )

        lifecycleScope.launch {
            try {
                RetroFitClient.getApiService(requireContext()).createPost(newPost)
                Toast.makeText(requireContext(), "Post submitted!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_newPostFragment_to_sistersFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
