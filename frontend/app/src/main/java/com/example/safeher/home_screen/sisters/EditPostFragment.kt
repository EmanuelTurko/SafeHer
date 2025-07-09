package com.example.safeher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.safeher.api.RetroFitClient
import com.example.safeher.R
import com.example.safeher.model.api.CreatePostRequest
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditPostFragment : Fragment() {

    private val args: EditPostFragmentArgs by navArgs()
    private lateinit var editTextPostContent: EditText
    private lateinit var buttonSavePost: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_post, container, false)
        editTextPostContent = view.findViewById(R.id.editTextPostContent)
        buttonSavePost     = view.findViewById(R.id.buttonSavePost)

        editTextPostContent.setText(args.currentBody)

        buttonSavePost.setOnClickListener {
            val newBody = editTextPostContent.text.toString().trim()
            if (newBody.isNotEmpty()) {
                updatePost(args.postId, newBody)
            } else {
                Toast.makeText(requireContext(),
                    "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }

    private fun updatePost(postId: String, body: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetroFitClient
                    .getApiService(requireContext())
                    .editPost(postId, CreatePostRequest(body = body))

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(),
                            "Post updated!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(requireContext(),
                            "Failed to update: ${response.code()}",
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(),
                        "Error: ${e.localizedMessage}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
