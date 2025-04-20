package com.example.safeher.bluetooth

enum class BluetoothState {
    IDLE,
    DISCONNECTED,
    SCANNING,
    SCAN_TIMEOUT,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    TRANSFER_INCOMPLETE
}