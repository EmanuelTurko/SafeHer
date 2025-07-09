package com.example.safeher.bluetooth

import android.bluetooth.BluetoothDevice

interface BluetoothCallback {
    fun onStateChanged(state: BluetoothState)
    fun onDeviceFound(device: BluetoothDevice)
    fun onServicesDiscovered()
    fun onCommandSent(command: String)
    fun onDataReceived(data: ByteArray, isCompleteFile:Boolean = false)
    fun onNotificationStatusChanged(enabled:Boolean,status: String)
    fun onError(error: BluetoothError, message:String)
    fun onTransferComplete()
    fun onMetadataReceived(totalImages: Int)
}