package com.example.practice6task7.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.practice6task7.ble.HeartRateBleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BleViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val bleManager = HeartRateBleManager(application)

    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState.asStateFlow()

    init {
        observeBleState()
    }

    private fun observeBleState() {
        viewModelScope.launch {
            combine(
                bleManager.devices,
                bleManager.isScanning,
                bleManager.connectionStatus,
                bleManager.heartRate,
                bleManager.errorMessage
            ) { devices, isScanning, connectionStatus, heartRate, errorMessage ->
                BleUiState(
                    devices = devices,
                    isScanning = isScanning,
                    connectionStatus = connectionStatus,
                    heartRate = heartRate,
                    errorMessage = errorMessage
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun restartScan() {
        bleManager.restartScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectToDevice(address: String) {
        bleManager.connectToDevice(address)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    fun clearError() {
        bleManager.clearError()
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.disconnect()
    }
}