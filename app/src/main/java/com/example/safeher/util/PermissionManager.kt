package com.example.safeher.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    companion object {
        const val REQUEST_CODE_SCAN = 1001
        const val REQUEST_CODE_CONNECT = 1002
        const val REQUEST_CODE_STORAGE = 1003
        const val REQUEST_CODE_ALL = 1004
    }

    private val baseBlePermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
    )
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    private val versionCodeSPermissions =  if(SdkVersion.VersionCodeS) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        emptyArray()
    }
    private val storagePermissions: Array<String>
        get() = when {
            SdkVersion.VersionCodeTiramisu -> {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            }
            true -> {
                arrayOf(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
            else -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }

    fun getScanPermissions(): Array<String>{
        return if (SdkVersion.VersionCodeS) {
            baseBlePermissions + versionCodeSPermissions
        } else {
            baseBlePermissions + locationPermissions
        }
    }
    fun getConnectPermissions(): Array<String> {
        return if(SdkVersion.VersionCodeS) {
            baseBlePermissions + versionCodeSPermissions
        } else {
            baseBlePermissions
        }
    }

    fun hasScanPermissions(): Boolean{
        return getScanPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    fun hasConnectPermissions(): Boolean{
        return getConnectPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    fun hasStoragePermissions(): Boolean {
        return storagePermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    fun hasAllPermissions(): Boolean {
        return hasScanPermissions() && hasConnectPermissions() && hasStoragePermissions()
    }
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun requestScanPermissions(requestCode: Int) {
        requestMissingPermissions(getScanPermissions(), requestCode)
    }
    fun requestConnectPermissions(requestCode: Int) {
        requestMissingPermissions(getConnectPermissions(), requestCode)
    }
    fun requestStoragePermissions(requestCode: Int) {
        if (SdkVersion.VersionCodeTiramisu) {
            // For Android 13 (API level 33), the READ_MEDIA_VIDEO permission is needed.
            val permissionsToRequest = storagePermissions.filter { permission ->
                !hasPermission(permission)
            }.toTypedArray()

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(context as Activity, permissionsToRequest, requestCode)
            }
        } else {
            // For Android versions below 10, we request the classic permissions.
            val permissionsToRequest = storagePermissions.filter { permission ->
                !hasPermission(permission)
            }.toTypedArray()

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(context as Activity, permissionsToRequest, requestCode)
            }
        }
    }

    private fun requestMissingPermissions(permissions: Array<String>, requestCode: Int){
        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(context as Activity, missingPermissions, requestCode)
        }
    }
    fun shouldShowScanRationale(): Boolean {
        return getScanPermissions().any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)
        }
    }
    fun shouldShowConnectRationale(): Boolean {
        return getConnectPermissions().any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)
        }
    }

}