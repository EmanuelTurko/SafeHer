package com.example.safeher.utils


import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.view.inputmethod.InputMethodManager

fun Activity.hideSoftKeyboard() {
    currentFocus?.let {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = currentFocus
        currentFocusedView?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }
}
fun Activity.setupUI(view: View) {
    if (view !is EditText) {
        view.setOnTouchListener { v, event ->
            hideSoftKeyboard()
            false
        }
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val childView = view.getChildAt(i)
            setupUI(childView)
        }
    }
}