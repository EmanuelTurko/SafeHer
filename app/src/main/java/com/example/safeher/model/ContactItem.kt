package com.example.safeher.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactItem(
    val name: String,
    val phoneNumber: String,
    var isSelected: Boolean = false
) : Parcelable
