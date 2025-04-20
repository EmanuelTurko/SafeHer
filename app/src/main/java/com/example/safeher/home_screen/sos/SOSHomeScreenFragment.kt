package com.example.safeher.home_screen.sos

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.settings.SettingsMainActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safeher.bluetooth.BluetoothController
import com.example.safeher.util.PermissionManager
import com.example.safeher.util.PermissionManager.Companion.REQUEST_CODE_STORAGE
import com.example.safeher.util.SdkVersion
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

class SOSHomeScreenFragment : Fragment() {


    private val bluetoothViewModel: BluetoothViewModelBLE by lazy {
        ViewModelProvider(this)[BluetoothViewModelBLE::class.java]
    }
    private lateinit var permissionManager: PermissionManager
    private lateinit var videoViewModel: VideoViewModel
    private var bSosActiveValue : Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    lateinit var mSistersButton: LinearLayout
    lateinit var mVideoLibraryButton: LinearLayout
    lateinit var mSupportCallButton: LinearLayout
    lateinit var mSosButton: ConstraintLayout
    lateinit var mHelperSwitch: SwitchMaterial
    lateinit var mHelperStatusText: TextView
    lateinit var mWelcomeText: TextView
    lateinit var mSettingsButtonCard: MaterialCardView
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val logoutSuccess = result.data?.getBooleanExtra("LOGOUT_SUCCESS", false) ?: false
            if(logoutSuccess) {
                activity?.finish()
            }

        }
    }
    private val requestCallPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                makePhoneCall()
            } else {
                Toast.makeText(requireActivity(), "Permission denied", Toast.LENGTH_SHORT).show()
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
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isBluetoothGranted = permissions[Manifest.permission.BLUETOOTH] == true
            val isLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            // For Android 12 and higher, Bluetooth permissions are handled separately
            val isBluetoothConnectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
            val isBluetoothScanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] == true

            if (isBluetoothGranted || (isBluetoothConnectGranted && isBluetoothScanGranted) && isLocationGranted) {
                Log.d("PermissionsLog", "Bluetooth and location permissions granted. Starting scan...")
                bluetoothViewModel.checkPermissionsAndScan()
            } else {
                Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show()
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBluetoothViewModel()
        videoViewModel = initVideoViewModel()
        bluetoothViewModel.checkPermissionsAndScan()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bluetoothViewModel.bleEvent.collect { event ->
                    when (event) {
                        is BleEvent.RequestPermissions -> {
                            Log.d("PermissionsLog", "Requesting permissions...")
                            requestPermissions()
                        }
                        is BleEvent.ShowRationale -> {
                            Log.d("PermissionsLog", "Showing rationale...")
                            showPermissionRationale()
                        }
                        is BleEvent.PromptBluetoothEnable -> {
                            Log.d("PermissionsLog", "Prompting to enable Bluetooth...")
                            promptEnableBluetooth()
                        }
                        is BleEvent.ReadyToScan -> {
                            Log.d("PermissionsLog", "Ready to scan...")
                            bluetoothViewModel.startScan()
                        }
                    }
                }
            }
        }
        bluetoothViewModel.isConverting.observe(viewLifecycleOwner) { isConverting ->
            bluetoothViewModel.imageData.observe(viewLifecycleOwner) { data ->
                Log.d("PermissionsLog", "Received image data: $data")

                if (isConverting == true) {
                    val saved = videoViewModel.videoManager.saveImageData(data, videoViewModel.frameIndex)
                    if (saved) {
                        videoViewModel.frameIndex++

                        if (bluetoothViewModel.imagesReceived >= bluetoothViewModel.totalImagesExpected) {
                            Log.d("PermissionsLog", "total ${bluetoothViewModel.totalImagesExpected}")
                            Log.d("PermissionsLog", "received ${bluetoothViewModel.imagesReceived}")
                            videoViewModel.processVideo()
                        }
                    }
                }
            }
        }
    }

    private fun initBluetoothViewModel(): BluetoothViewModelBLE {
        return ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val bluetoothController = BluetoothController(
                        requireContext().applicationContext,
                        permissionManager = PermissionManager(requireContext())
                    )
                    return BluetoothViewModelBLE(
                        bluetoothController,
                        permissionManager = PermissionManager(requireContext())
                    ) as T
                }
            }
        )[BluetoothViewModelBLE::class.java]
    }
    private fun initVideoViewModel(): VideoViewModel {
        return ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VideoViewModel(requireContext().applicationContext) as T
                }
            }
        )[VideoViewModel::class.java]
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
            findNavController().navigate(R.id.action_SOSHomeScreenFragment_to_sistersFragment)

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
            if(!bSosActiveValue){
                bluetoothViewModel.sendCommand("START")
                videoViewModel.cleanUpTempFiles()
                bSosActiveValue = true
            } else {
                bluetoothViewModel.sendCommand("STOP")
                bSosActiveValue = false
            }
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
            val intent = Intent(requireContext(), SettingsMainActivity::class.java)
            launcher.launch(intent)
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
    private fun showPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Required")
            .setMessage("This app needs Bluetooth and location permissions to discover nearby devices")
            .setPositiveButton("Grant") { _, _ ->
                permissionManager.requestScanPermissions(PermissionManager.REQUEST_CODE_SCAN)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun requestPermissions(){
        requestBluetoothPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        )
    }
    private fun promptEnableBluetooth(){
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(intent)
    }

}

