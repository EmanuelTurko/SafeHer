package com.example.safeher.home_screen.sos

import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.bluetooth.BluetoothCallback
import com.example.safeher.bluetooth.BluetoothController
import com.example.safeher.bluetooth.BluetoothError
import com.example.safeher.bluetooth.BluetoothState
import com.example.safeher.util.PermissionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BleEvent{
    object RequestPermissions : BleEvent()
    object showRationale : BleEvent()
    object promptBluetoothEnable : BleEvent()
    object ReadyToScan : BleEvent()
}

class BluetoothViewModelBLE @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val permissionManager: PermissionManager
    ) : ViewModel(), BluetoothCallback {


    init {
        checkPermissionsAndScan()
    }

    private val _bleEvent = MutableStateFlow<BleEvent>(BleEvent.RequestPermissions)
    val bleEvent: StateFlow<BleEvent> = _bleEvent

    private val _bluetoothState = MutableStateFlow<BluetoothState>(BluetoothState.IDLE)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error


    internal fun checkPermissionsAndScan() {
        when {
            !permissionManager.hasScanPermissions() -> {
                if (permissionManager.shouldShowScanRationale()) {
                    _bleEvent.value = BleEvent.showRationale
                } else {
                    _bleEvent.value = BleEvent.RequestPermissions
                }
            }
            bluetoothController.bluetoothAdapter?.isEnabled != true -> {
                _bleEvent.value = BleEvent.promptBluetoothEnable
            }
            else -> {
                _bleEvent.value = BleEvent.ReadyToScan
                startScan()
            }
        }
    }

    fun startScan(){
        bluetoothController.scanDevices()
    }
    fun connectToDevice(device: BluetoothDevice){
        bluetoothController.connectToDevice(device)
    }
    fun sendCommand(command:String){
        bluetoothController.sendCommand(command)
    }
    fun disconnect(){
        bluetoothController.disconnect()
    }

    override fun onStateChanged(state: BluetoothState) {
        _bluetoothState.value = state
    }


    override fun onDeviceFound(device: BluetoothDevice) {
        TODO("Not yet implemented")
    }

    override fun onError(error: BluetoothError, message: String) {
        viewModelScope.launch {
            _error.emit("$error: $message")
        }
    }

    override fun onTransferComplete() {
        TODO("Not yet implemented")
    }

    override fun onMetadataReceived(totalImages: Int) {
        TODO("Not yet implemented")
    }

    override fun onServicesDiscovered() {
        TODO("Not yet implemented")
    }
    override fun onCommandSent(command: String) {
        TODO("Not yet implemented")
    }
    override fun onDataReceived(data: ByteArray, isCompleteFile: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onNotificationStatusChanged(enabled: Boolean, status: String) {
        TODO("Not yet implemented")
    }
}
