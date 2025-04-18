package com.example.safeher.general

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ImageUtils {

    fun convertImageToBase64(inputStream: InputStream) : String? {
        try {
            //val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Convert the bitmap to a byte array
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // Convert byte array to Base64 string
            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

            return base64String
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun convertBase64ToBitmap(base64String: String): Bitmap? {
        try {
            // Decode the Base64 string
            val decodedString: ByteArray = Base64.decode(base64String, Base64.DEFAULT)

            // Convert the decoded byte array into a Bitmap
            val decodedByte: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

            // Set the Bitmap to the ImageView
            return  decodedByte
        } catch (e: Exception) {
            e.printStackTrace() // Handle any exceptions that occur
            return null
        }
    }

    fun convertBitmapToBase64(bitmap: Bitmap): String {
        // Create a ByteArrayOutputStream to hold the bytes of the Bitmap
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Compress the Bitmap into the output stream as PNG or JPEG (choose format as needed)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        // Convert the byte output stream to a byte array
        val byteArray = byteArrayOutputStream.toByteArray()

        // Encode the byte array to a Base64 string
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}