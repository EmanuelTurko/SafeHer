package com.example.safeher.home_screen.sos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class SOSHomeScreenFragment : Fragment() {

    lateinit var mSistersButton: LinearLayout
    lateinit var mVideoLibraryButton: LinearLayout
    lateinit var mSupportCallButton: LinearLayout
    lateinit var mSosButton: ConstraintLayout
    lateinit var mHelperSwitch: SwitchMaterial
    lateinit var mHelperStatusText: TextView
    lateinit var mWelcomeText: TextView
    lateinit var mSettingsButtonCard: MaterialCardView
    private val requestCallPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                makePhoneCall()
            } else {
                Toast.makeText(requireActivity(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sos_home_screen, container, false)
        initView(view)
        initListener()
        return view
    }

    private fun initView(view: View) {
        mSistersButton = view.findViewById(R.id.sistersButton)
        mVideoLibraryButton = view.findViewById(R.id.videoLibraryButton)
        mSupportCallButton = view.findViewById(R.id.supportCallButton)
        mSosButton = view.findViewById(R.id.sosButton)
        mHelperSwitch = view.findViewById(R.id.helperSwitch)
        mHelperStatusText = view.findViewById(R.id.helperStatusText)
        mWelcomeText = view.findViewById(R.id.welcomeText)
        mSettingsButtonCard = view.findViewById(R.id.settingsButtonCard)
    }

    private fun initListener() {
        mSistersButton.setOnClickListener {

        }

        mVideoLibraryButton.setOnClickListener {
            findNavController().navigate(R.id.action_SOSHomeScreenFragment_to_videoLibraryFragment)
        }

        mSupportCallButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall()
            } else {
                requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }

        mSosButton.setOnClickListener {

        }

        mHelperSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mHelperStatusText.text = "ON"
                // Do something when checked
            } else {
                mHelperStatusText.text = "OFF"
                // Do something when unchecked
            }
        }

        mSettingsButtonCard.setOnClickListener {

        }
    }

    private fun makePhoneCall() {
        val phoneNumber = "tel:0506000000"
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse(phoneNumber)

        try {
            startActivity(callIntent)
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(requireActivity(), "Call permission not granted", Toast.LENGTH_SHORT).show()
        }
    }
}