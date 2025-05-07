package com.example.safeher.home_screen.sos

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.bluetooth.BluetoothController
import com.example.safeher.home_screen.videoLibrary.VideoViewModel
import com.example.safeher.settings.SettingsMainActivity
import com.example.safeher.utils.PermissionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class SOSHomeScreenFragment : Fragment() {


    private lateinit var bluetoothViewModel: BluetoothViewModelBLE
    private lateinit var permissionManager: PermissionManager
    private lateinit var videoViewModel: VideoViewModel
    private var bSosActiveValue : Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    lateinit var mSosButtonContainer: MaterialCardView
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
        //bluetoothViewModel = ViewModelProvider(requireActivity())[BluetoothViewModelBLE::class.java]
        bluetoothViewModel = initBluetoothViewModel()
        //initBluetoothViewModel()
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
        bluetoothViewModel.imageData.observe(viewLifecycleOwner) { data ->
            val isConverting = bluetoothViewModel.isConverting.value

            if (isConverting == true) {
                Log.d("TestSample", "Image Recieved: ${bluetoothViewModel.imagesReceived}")
                Log.d("TestSample", "Image Total: ${bluetoothViewModel.totalImagesExpected}")
                val saved = videoViewModel.saveImageData(data, bluetoothViewModel.imagesReceived)
                if (saved) {
                    if (bluetoothViewModel.imagesReceived >= bluetoothViewModel.totalImagesExpected) {
                        videoViewModel.processVideo()
                        bluetoothViewModel.imagesReceived = 0
                        bluetoothViewModel.totalImagesExpected = 0
                    }
                }
            }
        }
    }

    private fun initBluetoothViewModel(): BluetoothViewModelBLE {
        return ViewModelProvider(
            requireActivity(),
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

        mSosButtonContainer = view.findViewById(R.id.sosButtonContainer)
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
            supportCallAlertBuilder()
           /* if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                makePhoneCall()
            } else {
                requestCallPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }*/
        }

        mSosButton.setOnClickListener {
            val colorOff = ContextCompat.getColor(requireContext(), R.color.sos_card_off)
            val colorOn = ContextCompat.getColor(requireContext(), R.color.sos_card_on)
            if(!bSosActiveValue){
                bluetoothViewModel.sendCommand("START")
                bSosActiveValue = true
                mSosButtonContainer.setCardBackgroundColor(colorOn)

            } else {
                bluetoothViewModel.sendCommand("STOP")
                bSosActiveValue = false
                Log.d("TestSample", "SOS button clicked")
                mSosButtonContainer.setCardBackgroundColor(colorOff)
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
    fun supportCallAlertBuilder(){
        AlertDialog.Builder(requireContext())
            .setTitle("Support Call")
            .setMessage("Do you want a support call?")
            .setPositiveButton("Human support") { dialog, _ ->
                //sister's logic

            }
            .setNegativeButton("Virtual intelligence support") { dialog, _ ->
                //AI logic
                findNavController().navigate(R.id.action_homePageFragment_to_supportCallAiFragment)
            }
            .setCancelable(true)
            .show()

    }

}

