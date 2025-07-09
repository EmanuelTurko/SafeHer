package com.example.safeher.general

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.example.safeher.R

class LoadingDialog(context: Context) : Dialog(context) {

    init {
        // Inflate the custom layout
        val inflater = LayoutInflater.from(context)
        val customView = inflater.inflate(R.layout.loading_dialog, null)

        // Set the custom layout to the dialog
        setContentView(customView)
        makeFullScreen()
        setGrayBackgroundWithAlpha()
    }

    // Set the background to gray with alpha transparency
    private fun setGrayBackgroundWithAlpha() {
        val window = window
        window?.let {
            // Set the background color of the dialog window to gray with alpha
            it.setBackgroundDrawable(ColorDrawable(Color.argb(150, 169, 169, 169))) // 150 alpha value for transparency
        }
    }

    // Set the dialog to full screen
    private fun makeFullScreen() {
        val window = window
        window?.let {
            it.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )  // Set width and height to match parent (full screen)
            it.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            )  // Set flag to make dialog full screen
        }
    }
}