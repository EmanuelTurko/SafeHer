package com.example.safeher.general

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.ByteArrayOutputStream
import java.io.InputStream

class ImagePickerHelper(
    private val fragment: Fragment,
    private val onImagePicked: (Bitmap?, Uri?) -> Unit,
    private val onPermissionDenied: () -> Unit
) {

    // Permission launcher
    private val requestPermissionLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (allPermissionsGranted) {
                openImageChooser()
            } else {
                onPermissionDenied()
            }
        }

    // Image picker launcher
    private val pickImageLauncher =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.let {
                    val selectedImage: Uri? = it.data
                    if (selectedImage != null) {
                        onImagePicked(null, selectedImage)
                    } else {
                        val photo: Bitmap = it.extras?.get("data") as Bitmap
                        onImagePicked(photo, null)
                    }
                }
            }
        }

    // Check if permissions are granted
    private fun checkPermissions(): Boolean {
        val permissionCamera = ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            Manifest.permission.CAMERA
        )
        return permissionCamera == PackageManager.PERMISSION_GRANTED
    }

    // Request permissions
    private fun requestPermissions() {
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
    }

    // Open image chooser
    fun openGallery() {
        if (checkPermissions()) {
            openImageChooser()
        } else {
            requestPermissions()
        }
    }

    private fun openImageChooser() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        val chooserIntent = Intent.createChooser(galleryIntent, "Select Image").apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        }

        pickImageLauncher.launch(chooserIntent)
    }
}
