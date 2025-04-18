package com.example.safeher.general

import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment

import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

fun Fragment.showDatePicker(onDateSelected: (String) -> Unit) {
    val materialDatePicker = MaterialDatePicker.Builder.datePicker()
        .setTitleText("Select Date")
        .build()

    materialDatePicker.show(requireActivity().supportFragmentManager, materialDatePicker.toString())

    materialDatePicker.addOnPositiveButtonClickListener { selection ->
        selection?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formattedDate = sdf.format(Date(it))
            onDateSelected(formattedDate)
        }
    }
}