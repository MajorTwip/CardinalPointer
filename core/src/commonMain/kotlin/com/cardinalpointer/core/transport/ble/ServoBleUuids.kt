package com.cardinalpointer.core.transport.ble

/**
 * BLE identifiers exposed by the ESP32 servo interface firmware.
 * Must stay in sync with `firmware/src/config.h`.
 */
object ServoBleUuids {
    const val DEVICE_NAME = "CardinalPointer-Servo"

    /** Primary GATT service. */
    const val SERVICE = "6b1a0001-8f3a-4b7d-9c2e-0a1b2c3d4e5f"

    /** App -> device. Write / Write-no-response. Carries ASCII command lines. */
    const val COMMAND = "6b1a0002-8f3a-4b7d-9c2e-0a1b2c3d4e5f"

    /** Device -> app. Read / Notify. Carries ASCII status lines. */
    const val STATUS = "6b1a0003-8f3a-4b7d-9c2e-0a1b2c3d4e5f"
}
