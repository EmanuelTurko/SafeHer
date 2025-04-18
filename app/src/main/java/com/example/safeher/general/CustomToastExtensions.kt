package com.example.safeher.general

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.example.safeher.R

fun Fragment.showCustomToast(message: String, title: String? = null, iconResId: Int? = null) {
    val inflater = LayoutInflater.from(context)
    val layout = inflater.inflate(R.layout.custom_toast, null, false)

    // Set dynamic values
    val toastIcon = layout.findViewById<AppCompatImageView>(R.id.custom_toast_image)
    val toastTitle = layout.findViewById<AppCompatTextView>(R.id.custom_toast_title)
    val toastMessage = layout.findViewById<AppCompatTextView>(R.id.custom_toast_message)

    iconResId?.let {
        toastIcon.setImageResource(it)
    }
    title?.let {
        toastTitle.text = title
    }
    toastMessage.text = message

    val toast = Toast(context)
    toast.duration = Toast.LENGTH_LONG
    toast.view = layout
    toast.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 100)
    toast.show()
}