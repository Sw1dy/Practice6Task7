package com.example.practice6task7.ble

import java.util.UUID

object BleConstants {
    val HEART_RATE_SERVICE_UUID: UUID =
        UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")

    val HEART_RATE_MEASUREMENT_UUID: UUID =
        UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}