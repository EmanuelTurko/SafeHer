package com.example.safeher.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.safeher.databinding.FragmentAboutAppBinding
import androidx.navigation.fragment.findNavController
import com.example.safeher.R

class AboutAppFragment : Fragment() {

    private var _binding: FragmentAboutAppBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutAppBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButtonCard.setOnClickListener {
            findNavController().navigate(R.id.action_aboutAppFragment_to_settingsLobbyFragment)
        }

        binding.aboutText.text = """
            Welcome to SafeHer – your trusted safety companion.

            SafeHer is a mobile application created to enhance personal safety for women by offering quick, reliable tools during emergencies. Whether you're walking alone, feeling unsafe, or just want someone to know where you are – SafeHer is here for you.

            Our Purpose:
            SafeHer was developed to empower users with a sense of control and security in vulnerable situations. By enabling fast, discreet communication with trusted contacts, the app provides immediate support when it matters most.

            Key Features:
            • Build your Safe Circle – choose people you trust to be alerted in case of emergency.
            • One-tap SOS – instantly send a location-based alert to your Safe Circle.
            • Contact Management – add, remove or update emergency contacts anytime.
            • Location Sharing – send real-time GPS location when SOS is activated.
            • Clean, intuitive design – minimal steps, maximum speed in critical moments.

            Our Mission:
            To make personal safety accessible, discreet, and effective – no matter where you are.

            Version: 1.0.0
            Developed by: The SafeHer Team
            Contact: support@safeher.app

            Thank you for using SafeHer and being part of a safer future.
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
