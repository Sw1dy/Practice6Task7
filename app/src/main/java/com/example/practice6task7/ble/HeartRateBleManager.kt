package com.example.practice6task7.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
class HeartRateBleManager(
    private val context: Context
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? =
        bluetoothManager.adapter

    private val scanner = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    val devices: StateFlow<List<BleDevice>> = _devices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address
            val name = device.name ?: "Unknown device"

            val bleDevice = BleDevice(
                name = name,
                address = address
            )

            val currentDevices = _devices.value

            if (currentDevices.none { it.address == address }) {
                _devices.value = currentDevices + bleDevice
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _errorMessage.value = "Scan failed: $errorCode"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionStatus.value = "Connected"
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionStatus.value = "Disconnected"
                _heartRate.value = null
                gatt.close()
            }
        }

        override fun onServicesDiscovered(
            gatt: BluetoothGatt,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _errorMessage.value = "Service discovery failed"
                return
            }

            val service = gatt.getService(BleConstants.HEART_RATE_SERVICE_UUID)

            if (service == null) {
                _errorMessage.value = "Heart Rate Service not found"
                return
            }

            val characteristic =
                service.getCharacteristic(BleConstants.HEART_RATE_MEASUREMENT_UUID)

            if (characteristic == null) {
                _errorMessage.value = "Heart Rate Measurement characteristic not found"
                return
            }

            enableHeartRateNotifications(gatt, characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == BleConstants.HEART_RATE_MEASUREMENT_UUID) {
                val value = characteristic.value
                val parsedHeartRate = parseHeartRate(value)
                _heartRate.value = parsedHeartRate
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (
                status == BluetoothGatt.GATT_SUCCESS &&
                characteristic.uuid == BleConstants.HEART_RATE_MEASUREMENT_UUID
            ) {
                val parsedHeartRate = parseHeartRate(characteristic.value)
                _heartRate.value = parsedHeartRate
            }
        }
    }

    fun startScan() {
        if (!hasBluetoothPermissions()) {
            _errorMessage.value = "Bluetooth permissions are not granted"
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            _errorMessage.value = "Bluetooth is disabled"
            return
        }

        _devices.value = emptyList()
        _errorMessage.value = null
        _isScanning.value = true

        scanner?.startScan(scanCallback)
    }

    fun stopScan() {
        if (!hasBluetoothPermissions()) return

        scanner?.stopScan(scanCallback)
        _isScanning.value = false
    }

    fun restartScan() {
        stopScan()
        startScan()
    }

    fun connectToDevice(address: String) {
        if (!hasBluetoothPermissions()) {
            _errorMessage.value = "Bluetooth permissions are not granted"
            return
        }

        stopScan()

        val device = bluetoothAdapter?.getRemoteDevice(address)

        if (device == null) {
            _errorMessage.value = "Device not found"
            return
        }

        _connectionStatus.value = "Connecting"
        _heartRate.value = null

        bluetoothGatt = device.connectGatt(
            context,
            false,
            gattCallback
        )
    }

    fun disconnect() {
        if (!hasBluetoothPermissions()) return

        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null

        _connectionStatus.value = "Disconnected"
        _heartRate.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun enableHeartRateNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(
            BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID
        )

        if (descriptor == null) {
            _errorMessage.value = "CCC descriptor not found"
            return
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    private fun parseHeartRate(value: ByteArray?): Int? {
        if (value == null || value.isEmpty()) return null

        val flags = value[0].toInt()
        val isHeartRate16Bit = flags and 0x01 != 0

        return if (isHeartRate16Bit) {
            if (value.size < 3) return null

            ((value[2].toInt() and 0xFF) shl 8) or
                    (value[1].toInt() and 0xFF)
        } else {
            if (value.size < 2) return null

            value[1].toInt() and 0xFF
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}