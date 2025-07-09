package com.example.safeher.general

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.example.safeher.R

class ErrorDialog(private val context: Context) {

    lateinit var titleText : AppCompatTextView
    lateinit var messageText : AppCompatTextView
    lateinit var actionButton : AppCompatButton


    fun show(
        title: String,
        message: String,
        buttonText: String,
        onButtonClick: () -> Unit = {}
    ) {
        val dialog = Dialog(context)

        val view = LayoutInflater.from(context).inflate(R.layout.general_error_popup, null)

        titleText = view.findViewById(R.id.popupTitle)
        messageText = view.findViewById(R.id.popupMessage)
        actionButton = view.findViewById(R.id.errorButton)

        // Set content
        titleText.text = title
        messageText.text = message
        actionButton.text = buttonText


        // Set button click listener
        actionButton.setOnClickListener {
            onButtonClick()
            dialog.dismiss()
        }

        // Configure dialog window
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }
}