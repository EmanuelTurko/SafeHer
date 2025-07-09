package com.example.safeher.bluetooth

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.safeher.utils.SdkVersion
import com.example.safeher.utils.PermissionManager
import java.io.ByteArrayOutputStream
import java.util.UUID

class BluetoothController(
    private val context: Context,
    private val permissionManager: PermissionManager) {

    private val serviceUUID = UUID.fromString("ABCD0001-0000-1000-8000-00805F9B34FB")
    private val commandUUID = UUID.fromString("ABCD0002-0000-1000-8000-00805F9B34FB")
    private val dataUUID = UUID.fromString("ABCD0003-0000-1000-8000-00805F9B34FB")

    private var callback: BluetoothCallback? = null
    private val bluetoothManager by lazy { context.getSystemService(BluetoothManager::class.java) }
    val bluetoothAdapter by lazy { bluetoothManager?.adapter }
    private val bluetoothLeScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private var bluetoothGatt: BluetoothGatt? = null
    var commandCharacteristic: BluetoothGattCharacteristic? = null
    var dataCharacteristic: BluetoothGattCharacteristic? = null

    private val handler = Handler(Looper.getMainLooper())
    private var scanTimeoutRunnable: Runnable? = null

    private var expectedFileSize = 0
    private var currentFileSize = 0
    private val fileBuffer = ByteArrayOutputStream()
    private var totalImagesExpected = 0
    private var imagesReceived = 0

    @Synchronized
    fun scanDevices() {
        when {
            bluetoothAdapter?.isEnabled != true -> {
                callback?.onError(BluetoothError.BLUETOOTH_DISABLED, "Bluetooth is disabled")
            }

            !permissionManager.hasScanPermissions() -> {
                callback?.onError(BluetoothError.PERMISSION_DENIED, "Scan permissions required")
                permissionManager.requestScanPermissions(PermissionManager.REQUEST_CODE_SCAN)
            }

            else -> {
                try {
                    stopScan()
                    bluetoothLeScanner?.startScan(scanCallback)
                    startScanTimeout()
                    callback?.onStateChanged(BluetoothState.SCANNING)
                } catch (e: SecurityException) {
                    handleSecurityException(e, "scan")
                }
            }
        }
    }

    @Synchronized
    fun connectToDevice(device: BluetoothDevice) {
        when {
            !permissionManager.hasConnectPermissions() -> {
                callback?.onError(BluetoothError.PERMISSION_DENIED, "Connect Permissions required")
                permissionManager.requestConnectPermissions(PermissionManager.REQUEST_CODE_CONNECT)
            }

            else -> {
                try {
                    disconnect()
                    bluetoothGatt = device.connectGatt(
                        context, false, gattCallback, BluetoothDevice.TRANSPORT_LE,
                        BluetoothDevice.PHY_LE_1M_MASK
                    )
                    callback?.onStateChanged(BluetoothState.CONNECTING)
                } catch (e: SecurityException) {
                    handleSecurityException(e, "connect")
                }
            }
        }
    }

    @Synchronized
    fun sendCommand(command: String) {
        Log.d("BLETesting", "sendCommand: $command")
        commandCharacteristic?.let { char ->
            try {
                val value = command.toByteArray()
                Log.d("BLETesting", "Sending command: $command (bytes: ${value.joinToString()})")
                if (SdkVersion.VersionCodeTiramisu) {
                    bluetoothGatt?.writeCharacteristic(
                        char,
                        value,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    )
                } else {
                    @Suppress("DEPRECATION")
                    char.value = value
                    @Suppress("DEPRECATION")
                    bluetoothGatt?.writeCharacteristic(char)
                }
                callback?.onCommandSent(command)
            } catch (e: SecurityException) {
                handleSecurityException(e, "send command")
            }
        } ?: run {
            callback?.onError(
                BluetoothError.CHARACTERISTIC_NOT_FOUND,
                "Command characteristic not available"
            )
        }
    }

    @Synchronized
    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
                bluetoothGatt = null
                commandCharacteristic = null
                dataCharacteristic = null
                callback?.onStateChanged(BluetoothState.DISCONNECTED)
            } catch (e: SecurityException) {
                callback?.onError(
                    BluetoothError.DISCONNECT_FAILED,
                    "Failed to disconnect properly: ${e.message}"
                )
            }
        }
    }

    private fun startScanTimeout() {
        scanTimeoutRunnable?.let { handler.removeCallbacks(it) }
        scanTimeoutRunnable = Runnable {
            stopScan()
            callback?.onStateChanged(BluetoothState.SCAN_TIMEOUT)
        }.also { handler.postDelayed(it, SdkVersion.scanTimeoutMs) }
    }

    @Synchronized
    private fun stopScan() {
        try {
            bluetoothLeScanner
            bluetoothLeScanner?.stopScan(scanCallback)
            scanTimeoutRunnable?.let { handler.removeCallbacks(it) }
        } catch (e: SecurityException) {
            callback?.onError(BluetoothError.SCAN_FAILED, "Failed to stop scan: ${e.message}")
        }
    }

    private fun handleSecurityException(e: SecurityException, operation: String) {
        callback?.onError(
            BluetoothError.SECURITY_ERROR,
            "Security exception during ${operation}:${e.message}"
        )
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                result.device?.let { device ->
                    if (device.name?.contains("SimpleBLE") == true ||
                        result.scanRecord?.serviceUuids?.any {
                            it.toString().equals(serviceUUID.toString(), true)
                        } == true
                    ) {
                        Log.d("Bluetooth", "found")
                        stopScan()
                        connectToDevice(device)
                    }
                }
            } catch (e: SecurityException) {
                callback?.onError(BluetoothError.PERMISSION_DENIED, ":${e.message}")
            }
        }


        override fun onScanFailed(errorCode: Int) {
            callback?.onError(
                BluetoothError.SCAN_FAILED,
                "Scan failed with error: ${scanErrorToString(errorCode)}"
            )
        }
    }

    private fun scanErrorToString(errorCode: Int): String {
        return when (errorCode) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
            else -> "Unknown error ($errorCode)"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d("BLETesting", "Connection state changed: $status, $newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BLETesting", "Connected to device ${gatt.device.address}")
                    handler.post {
                        try {
                            var value = false
                            gatt.requestMtu(517)
                            Log.d("BLETesting", "Starting service discovery...")
                            handler.postDelayed({
                                value = gatt.discoverServices()
                            }, 1000)
                            Log.d("BLETesting", "returned: $value")
                            callback?.onStateChanged(BluetoothState.CONNECTED)
                        } catch (e: SecurityException) {
                            handleSecurityException(e, "service discovery")
                        }
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {

                    handler.post {
                        callback?.onStateChanged(BluetoothState.DISCONNECTED)
                        disconnect()
                    }
                }
            }
        }
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d("BLETesting", "New MTU: $mtu")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    gatt.services?.forEach { service ->
                        service.characteristics?.forEach { char ->
                            Log.d("BLETesting", "  Characteristic: ${char.uuid}")
                        }
                    }
                    val service = gatt.getService(serviceUUID)
                    commandCharacteristic = service?.getCharacteristic(commandUUID)
                    Log.d("BLETesting", "Command characteristic found: $commandCharacteristic")
                    dataCharacteristic = service?.getCharacteristic(dataUUID)

                    if (commandCharacteristic != null && dataCharacteristic != null) {
                        handler.postDelayed({
                            enableNotifications(gatt)
                        },300)
                        callback?.onServicesDiscovered()
                    } else {
                        callback?.onError(
                            BluetoothError.SERVICE_DISCOVERY_FAILED,
                            "Required characteristics not found"
                        )
                    }
                }
                else -> {
                    callback?.onError(
                        BluetoothError.SERVICE_DISCOVERY_FAILED,
                        "Service discovery failed: $status"
                    )
                }
            }
        }
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            try {
                when (characteristic.uuid) {
                    commandUUID -> handleCommandCharacteristic(gatt, characteristic)
                    dataUUID -> handleDataCharacteristic(gatt, characteristic)
                    else -> {
                        Log.d(
                            "BLETesting",
                            "Unknown characteristic changed: ${characteristic.uuid}"
                        )
                        return
                    }
                }
            } catch (e: Exception) {
                handleDataError(e)
            }
        }
        private fun handleCommandCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            try {

                if (SdkVersion.VersionCodeTiramisu) {
                    if (!permissionManager.hasConnectPermissions()) {
                        callback?.onError(
                            BluetoothError.PERMISSION_DENIED,
                            "Missing permissions to read characteristic"
                        )
                        return
                    }
                    if (!gatt.readCharacteristic(characteristic)) {
                        callback?.onError(
                            BluetoothError.DATA_ERROR,
                            "Failed to read command characteristic"
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    handleCommandValue(characteristic.value)
                }
            } catch (e: SecurityException) {
                callback?.onError(
                    BluetoothError.PERMISSION_DENIED,
                    "Permission denied: ${e.message}"
                )
            }
        }
        private fun handleDataCharacteristic(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            try {

                if (SdkVersion.VersionCodeTiramisu) {
                    if (!permissionManager.hasConnectPermissions()) {
                        callback?.onError(
                            BluetoothError.PERMISSION_DENIED,
                            "Missing permissions to read characteristic"
                        )
                        return
                    }
                    if (!gatt.readCharacteristic(characteristic)) {
                        callback?.onError(
                            BluetoothError.DATA_ERROR,
                            "Failed to read data characteristic"
                        )
                    }
                } else {
                    @Suppress("DEPRECATION")
                    handleDataValue(characteristic.value)
                }
            } catch(e: SecurityException){
                callback?.onError(BluetoothError.PERMISSION_DENIED, "Permission denied: ${e.message}"
                )
            }
        }
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    callback?.onError(
                        BluetoothError.DATA_ERROR,
                        "Failed to read characteristic (status $status)"
                    )
                    return
                }

                when (characteristic.uuid) {
                    commandUUID -> handleCommandValue(value)
                    dataUUID -> handleDataValue(value)
                }
            } catch (e: Exception) {
                handleDataError(e)
            }
        }
        private fun handleCommandValue(value: ByteArray?) {
            val data = value ?: run {
                callback?.onError(BluetoothError.DATA_ERROR, "Null command value")
                return
            }

            Log.d("BLETesting", "CMD RAW: ${data.joinToString { "%02X".format(it) }}")

            if (data.size == 2) {
                totalImagesExpected = (data[0].toInt() and 0xFF shl 8) or
                        (data[1].toInt() and 0xFF)
                Log.d("BLETesting", "Image count received: $totalImagesExpected")
                callback?.onMetadataReceived(totalImagesExpected)
            } else {
                Log.w("BLETesting", "Unexpected command size: ${data.size} bytes")
            }
        }

        private fun handleDataValue(value: ByteArray?) {
            val data = value ?: run {
                callback?.onError(BluetoothError.DATA_ERROR, "Null data received")
                return
            }
            if (expectedFileSize == 0 && data.size >= 4) {
                expectedFileSize = (data[0].toInt() and 0xFF shl 24) or
                        (data[1].toInt() and 0xFF shl 16) or
                        (data[2].toInt() and 0xFF shl 8) or
                        (data[3].toInt() and 0xFF)

                Log.d("BLETesting", "New file header detected, size: $expectedFileSize bytes")

                //if data left after header
                if (data.size > 4) {
                    fileBuffer.write(data, 4, data.size - 4)
                    currentFileSize = data.size - 4
                    Log.d("BLETesting", "Initial payload: ${data.size - 4} bytes")
                }
                return
            }

            fileBuffer.write(data)
            currentFileSize += data.size
            if (currentFileSize >= expectedFileSize && expectedFileSize > 0) {
                val imageData = fileBuffer.toByteArray().copyOf(expectedFileSize)

                Log.d("BLETesting", "Complete file received (${imageData.size} bytes)")
                callback?.onDataReceived(imageData, true)
                imagesReceived++
                sendCommand("RECEIVED")
                if(imagesReceived >= totalImagesExpected) {
                    Log.d("BLETesting", "All images received, resetting state")
                    callback?.onTransferComplete()
                }
                fileBuffer.reset()
                expectedFileSize = 0
                currentFileSize = 0
            }
        }
        private fun handleDataError(e: Exception) {
            when (e) {
                is SecurityException -> handleSecurityException(e, "data handling")
                else -> callback?.onError(
                    BluetoothError.DATA_ERROR,
                    "Error processing data: ${e.message}"
                )
            }
            resetTransferState()
        }

        private fun resetTransferState() {
            fileBuffer.reset()
            expectedFileSize = 0
            currentFileSize = 0
        }

        private fun enableNotifications(gatt: BluetoothGatt) {
            dataCharacteristic?.let { dataChar ->
                setNotificationForCharacteristic(gatt, dataChar, "DATA")
            } ?: run {
                callback?.onError(BluetoothError.CONNECTION_FAILED, "Data characteristic not found")
            }
            commandCharacteristic?.let { cmdChar ->
                setNotificationForCharacteristic(gatt, cmdChar, "COMMAND")
            } ?: run {
                callback?.onError(BluetoothError.CONNECTION_FAILED, "Command characteristic not found")
            }
        }
        private fun setNotificationForCharacteristic(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            type: String
        ) {
            try {
                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    callback?.onNotificationStatusChanged(false, "Failed to enable $type notifications")
                    return
                }
                val descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805F9B34FB") // Standard CCC UUID
                ) ?: run {
                    callback?.onNotificationStatusChanged(false, "$type CCC descriptor missing")
                    return
                }
                if (SdkVersion.VersionCodeTiramisu) {
                    gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
                Log.d("BLETesting", "Successfully enabled $type notifications")
            } catch (e: SecurityException) {
                handleSecurityException(e, "enable $type notifications")
            } catch (e: Exception) {
                callback?.onError(BluetoothError.CONNECTION_FAILED,
                    "$type notification failed: ${e.message}")
            }
        }
    }
    fun setCallback(callback: BluetoothCallback) {
        this.callback = callback
    }
}


