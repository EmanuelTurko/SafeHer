package com.example.safeher.home_screen.sos

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import kotlinx.coroutines.launch

class SOSHomeScreenFragment : Fragment() {

    private lateinit var bluetoothViewModel : BluetoothViewModelBLE
    private lateinit var permissionManager: PermissionManager
    private var bSosActiveValue : Boolean = false


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
            if(permissions[Manifest.permission.BLUETOOTH] == true &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
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
        bluetoothViewModel = initBluetoothViewModel()

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bluetoothViewModel.bleEvent.collect { event ->
                    when (event) {
                        is BleEvent.RequestPermissions -> {
                            requestPermissions()
                        }
                        is BleEvent.showRationale -> {
                            showPermissionRationale()
                        }
                        is BleEvent.promptBluetoothEnable -> {
                            promptEnableBluetooth()
                        }
                        is BleEvent.ReadyToScan -> {
                            bluetoothViewModel.startScan()
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
            } else {
                bluetoothViewModel.sendCommand("STOP")
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
                Manifest.permission.BLUETOOTH,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )
    }
    private fun promptEnableBluetooth(){
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(intent)
    }

}