package com.example.safeher.home_screen.sos

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.bluetooth.BluetoothCallback
import com.example.safeher.bluetooth.BluetoothController
import com.example.safeher.bluetooth.BluetoothError
import com.example.safeher.bluetooth.BluetoothState
import com.example.safeher.utils.PermissionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BleEvent{
    object RequestPermissions : BleEvent()
    object ShowRationale : BleEvent()
    object PromptBluetoothEnable : BleEvent()
    object ReadyToScan : BleEvent()
}

class BluetoothViewModelBLE @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val permissionManager: PermissionManager
    ) : ViewModel(), BluetoothCallback {

    init {
        bluetoothController.setCallback(this)
    }


    private val _bleEvent = MutableStateFlow<BleEvent>(BleEvent.RequestPermissions)
    val bleEvent: StateFlow<BleEvent> = _bleEvent

    private val _bluetoothState = MutableStateFlow<BluetoothState>(BluetoothState.IDLE)
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error

    private val _imageData = MutableLiveData<ByteArray>()
    val imageData: LiveData<ByteArray> = _imageData

    private var _isConverting = MutableLiveData<Boolean>(true)
    val isConverting: LiveData<Boolean> = _isConverting


    var totalImagesExpected: Int = 0
    var imagesReceived: Int = 0


    @Synchronized
     fun checkPermissionsAndScan() {
         Log.d("PermissionsLog", "Checking permissions and scan")
        when {
            !permissionManager.hasScanPermissions() -> {
                Log.d("PermissionsLog", "No permissions granted")
                if (permissionManager.shouldShowScanRationale()) {
                    Log.d("PermissionsLog", "Show rationale for permissions")
                    _bleEvent.value = BleEvent.ShowRationale
                } else {
                    Log.d("PermissionsLog", "Requesting permissions")
                    _bleEvent.value = BleEvent.RequestPermissions
                }
            }
            bluetoothController.bluetoothAdapter?.isEnabled != true -> {
                Log.d("PermissionsLog", "Bluetooth is not enabled")
                _bleEvent.value = BleEvent.PromptBluetoothEnable
            }
            else -> {
                Log.d("PermissionsLog", "Bluetooth is enabled and permissions are granted")
                _bleEvent.value = BleEvent.ReadyToScan
                startScan()
            }
        }
    }
    @Synchronized
    fun startScan(){
        bluetoothController.scanDevices()
    }
    fun connectToDevice(device: BluetoothDevice){
        bluetoothController.connectToDevice(device)
    }
    @Synchronized
    fun sendCommand(command:String){
        bluetoothController.sendCommand(command)
        _isConverting.value = _isConverting.value != true
        Log.d("PermissionsLog", "converting value is: ${_isConverting.value}")
    }
    fun disconnect(){
        bluetoothController.disconnect()
    }

    override fun onStateChanged(state: BluetoothState) {
        _bluetoothState.value = state
    }


    override fun onDeviceFound(device: BluetoothDevice) {
        Log.d("PermissionsLog", "Device found: $device - ${device.address}")
    }

    override fun onError(error: BluetoothError, message: String) {
        viewModelScope.launch {
            _error.emit("$error: $message")
        }
    }

    override fun onTransferComplete() {
        Log.d("PermissionsLog", "Transfer complete")
    }

    override fun onMetadataReceived(totalImages: Int) {
        totalImagesExpected = totalImages
    }

    override fun onServicesDiscovered() {
        Log.d("PermissionsLog", "Services discovered")
    }
    override fun onCommandSent(command: String) {
        Log.d("PermissionsLog", "Command sent: $command")
    }
    override fun onDataReceived(data: ByteArray, isCompleteFile: Boolean) {
        if(isCompleteFile){
            imagesReceived++
            _imageData.postValue(data)
        }
    }

    override fun onNotificationStatusChanged(enabled: Boolean, status: String) {
        Log.d("PermissionsLog", "Notification status changed: $enabled, $status")
    }
}
