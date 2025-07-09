package com.example.safeher.model.api


data class TwilioEmergencyMessageRequest(
    val userPhoneNumber: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)
