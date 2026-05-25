package com.example.practice6task7.presentation

import com.example.practice6task7.ble.BleDevice

data class BleUiState(
    val devices: List<BleDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val heartRate: Int? = null,
    val errorMessage: String? = null
)