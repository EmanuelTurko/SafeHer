package com.example.safeher.home_screen

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.safeher.R
import com.example.safeher.bluetooth.BluetoothController
import com.example.safeher.home_screen.sos.BluetoothViewModelBLE
import com.example.safeher.utils.PermissionManager
import com.example.safeher.utils.hideSystemUI
import com.example.safeher.utils.setupUI

class HomeScreenActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    //private lateinit var bluetoothViewModel: BluetoothViewModelBLE
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)
        // Obtain reference to the NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_home_screen_nav_host_fragment) as NavHostFragment
        // Get the NavController
        navController = navHostFragment.navController
        /*bluetoothViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val permissionManager = PermissionManager(this@HomeScreenActivity)
                    val bluetoothController = BluetoothController(
                        this@HomeScreenActivity.applicationContext,
                        permissionManager
                    )
                    return BluetoothViewModelBLE(bluetoothController, permissionManager) as T
                }
            }
        )[BluetoothViewModelBLE::class.java]*/
        setupUI(findViewById(android.R.id.content))
        hideSystemUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}