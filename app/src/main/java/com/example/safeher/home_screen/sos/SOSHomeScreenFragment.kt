package com.example.safeher.home_screen.sos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.api.RetroFitClient
import com.example.safeher.bluetooth.BluetoothController
import com.example.safeher.bluetooth.BluetoothState
import com.example.safeher.general.showCustomToast
import com.example.safeher.home_screen.videoLibrary.VideoViewModel
import com.example.safeher.model.api.TwilioEmergencyMessageRequest
import com.example.safeher.settings.SettingsMainActivity
import com.example.safeher.utils.PermissionManager
import com.example.safeher.utils.getStringShareRef
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.concurrent.thread

class SOSHomeScreenFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bluetoothViewModel: BluetoothViewModelBLE
    private lateinit var permissionManager: PermissionManager
    private lateinit var videoViewModel: VideoViewModel

    private lateinit var userPhoneNumber: String
    private var ignoreListener = false
    private var sosActive = false

    // UI
    private lateinit var mSosButtonContainer: CardView
    private lateinit var mSosButton: androidx.appcompat.widget.AppCompatImageView
    private lateinit var mSistersButton: LinearLayout
    private lateinit var mVideoLibraryButton: LinearLayout
    private lateinit var mSupportCallButton: LinearLayout
    private lateinit var mHelperSwitch: SwitchMaterial
    private lateinit var mHelperStatusText: TextView
    private lateinit var mWelcomeText: TextView
    private lateinit var mSettingsButtonCard: CardView

    // Activity-result launchers
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val logout = result.data?.getBooleanExtra("LOGOUT_SUCCESS", false) ?: false
            if (logout) activity?.finish()
        }
    }
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                bluetoothViewModel.checkPermissionsAndScan()
            } else {
                Toast.makeText(requireContext(), "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        }
    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val ok = perms[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                    perms[Manifest.permission.BLUETOOTH_SCAN] == true &&
                    perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (ok) bluetoothViewModel.checkPermissionsAndScan()
            else Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) getLastLocation()
            else Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }

    override fun onStart() {
        super.onStart()
        updateWelcomeText()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sos_home_screen, container, false).also { view ->
            initView(view)
            initListeners()
            updateWelcomeText()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        bluetoothViewModel = initBluetoothViewModel()
        videoViewModel     = initVideoViewModel()
        bluetoothViewModel.checkPermissionsAndScan()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bluetoothViewModel.bleEvent.collect { event ->
                    when (event) {
                        is BleEvent.RequestPermissions        -> requestPermissions()
                        is BleEvent.ShowRationale             -> showPermissionRationale()
                        is BleEvent.PromptBluetoothEnable     -> promptEnableBluetooth()
                        is BleEvent.ReadyToScan               -> bluetoothViewModel.startScan()
                    }
                }
            }
        }

        bluetoothViewModel.imageData.observe(viewLifecycleOwner) { data ->
            if (bluetoothViewModel.isConverting.value == true) {
                val saved = videoViewModel.saveImageData(data, bluetoothViewModel.imagesReceived)
                if (saved && bluetoothViewModel.imagesReceived >= bluetoothViewModel.totalImagesExpected) {
                    videoViewModel.processVideo()
                    bluetoothViewModel.imagesReceived = 0
                    bluetoothViewModel.totalImagesExpected = 0
                }
            }
        }
    }

    private fun initView(view: View) {
        mSosButtonContainer  = view.findViewById(R.id.sosButtonContainer)
        mSosButton           = view.findViewById(R.id.sosButton)
        mSistersButton       = view.findViewById(R.id.sistersButton)
        mVideoLibraryButton  = view.findViewById(R.id.videoLibraryButton)
        mSupportCallButton   = view.findViewById(R.id.supportCallButton)
        mHelperSwitch        = view.findViewById(R.id.helperSwitch)
        mHelperStatusText    = view.findViewById(R.id.helperStatusText)
        mWelcomeText         = view.findViewById(R.id.welcomeText)
        mSettingsButtonCard  = view.findViewById(R.id.settingsButtonCard)
        userPhoneNumber = requireContext().getStringShareRef("phoneNumber", "userInfo")
    }

    private fun initListeners() {
        // Slide-to-start/stop now replaced by tap:
        mSosButtonContainer.setOnClickListener { toggleSos() }

        mSistersButton.setOnClickListener {
            findNavController().navigate(R.id.action_SOSHomeScreenFragment_to_sistersFragment)
        }
        mVideoLibraryButton.setOnClickListener {
            findNavController().navigate(R.id.action_SOSHomeScreenFragment_to_videoLibraryFragment)
        }
        mSupportCallButton.setOnClickListener {
            supportCallAlertBuilder()
        }
        mSettingsButtonCard.setOnClickListener {
            launcher.launch(Intent(requireContext(), SettingsMainActivity::class.java))
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bluetoothViewModel.bluetoothState.collect { state ->
                    val isConnected = (state == BluetoothState.CONNECTED)
                    if (mHelperSwitch.isChecked != isConnected) {
                        ignoreListener = true
                        mHelperSwitch.isChecked = isConnected
                        ignoreListener = false
                    }
                }
            }
        }
        mHelperSwitch.setOnCheckedChangeListener { _, isChecked ->
            mHelperStatusText.text = if (isChecked) "ON" else "OFF"
            if (ignoreListener) return@setOnCheckedChangeListener
            if (isChecked) {
                if (bluetoothViewModel.bluetoothState.value != BluetoothState.CONNECTED) {
                    bluetoothViewModel.startScan()
                }
            } else {
                bluetoothViewModel.disconnect()
            }
        }
    }


    private fun updateWelcomeText() {
        val name = requireContext()
            .getSharedPreferences("userInfo", Context.MODE_PRIVATE)
            .getString("fullName", "")
            .orEmpty()
        mWelcomeText.text = "Welcome $name!"
    }

    private fun requestPermissions() {
        requestBluetoothPermissionLauncher.launch(arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("Bluetooth and location are needed.")
            .setPositiveButton("Grant") { _, _ ->
                permissionManager.requestScanPermissions(PermissionManager.REQUEST_CODE_SCAN)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptEnableBluetooth() {
        enableBluetoothLauncher.launch(
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        )
    }

    private fun supportCallAlertBuilder() {
        AlertDialog.Builder(requireContext())
            .setTitle("Support Call")
            .setMessage("Do you want a support call?")
            .setPositiveButton("Human support") { _, _ -> /* ... */ }
            .setNegativeButton("Virtual support") { _, _ ->
                findNavController().navigate(R.id.action_homePageFragment_to_supportCallAiFragment)
            }
            .show()
    }

    private fun initBluetoothViewModel(): BluetoothViewModelBLE {
        permissionManager = PermissionManager(requireContext())
        return ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val c = BluetoothController(requireContext().applicationContext, permissionManager)
                return BluetoothViewModelBLE(c, permissionManager) as T
            }
        })[BluetoothViewModelBLE::class.java]
    }

    private fun initVideoViewModel(): VideoViewModel {
        return ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VideoViewModel(requireContext().applicationContext) as T
            }
        })[VideoViewModel::class.java]
    }

    private fun toggleSos() {
        val colorOff = ContextCompat.getColor(requireContext(), R.color.sos_card_off)
        val colorOn  = ContextCompat.getColor(requireContext(), R.color.sos_card_on)
        if (!sosActive) {
            checkAndRequestLocationPermission()
            bluetoothViewModel.sendCommand("START")
            sosActive = true
            mSosButtonContainer.setCardBackgroundColor(colorOn)
        } else {
            bluetoothViewModel.sendCommand("STOP")
            sosActive = false
            mSosButtonContainer.setCardBackgroundColor(colorOff)
        }
    }
    private fun checkAndRequestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> getLastLocation()
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> AlertDialog.Builder(requireContext())
                .setTitle("Location Permission Needed")
                .setMessage("Location is needed to use this feature.")
                .setPositiveButton("OK") { _, _ ->
                    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                .setNegativeButton("Cancel", null)
                .show()
            else -> requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LocationDebug", "Permission not granted")
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    Log.d("LocationDebug", "Lat: ${it.latitude}, Lon: ${it.longitude}")
                    thread {
                        val address = Geocoder(requireContext(), Locale.getDefault())
                            .getFromLocation(it.latitude, it.longitude, 1)
                            ?.firstOrNull()?.getAddressLine(0) ?: "No address"
                        activity?.runOnUiThread {
                            Log.d("LocationDebug", "Address: $address")
                            sendEmergencyMessage(userPhoneNumber,address, it.latitude, it.longitude)
                        }
                    }
                } ?: Log.d("LocationDebug", "Location is null")
            }
            .addOnFailureListener {
                Log.d("LocationDebug", "Failed to get location")
            }
    }
    private fun sendEmergencyMessage(
        userPhoneNumber: String,
        address: String,
        latitude: Double,
        longitude: Double
    ){
        val request = TwilioEmergencyMessageRequest(
            userPhoneNumber,
            address,
            latitude,
            longitude
        )
        lifecycleScope.launch{
            try{
                val response = RetroFitClient.getApiService(requireContext()).sendEmergencyMessage(request)
                if (response.error.isNullOrEmpty()) {
                    Log.d("LocationDebug", "Emergency message sent successfully: ${response.message}")
                } else {
                    Log.e("LocationDebug", "Error sending emergency message: ${response.error}")
                }
            } catch (e: Exception) {
                Log.e("LocationDebug", "Exception: ${e.message}", e)
            }
        }
    }
}
