package com.cardinalpointer.core.transport.ble

import kotlinx.coroutines.flow.Flow

/**
 * Minimal, platform-agnostic view of a connected BLE peripheral that speaks the
 * servo protocol. Platform shells provide the concrete implementation (Android
 * `BluetoothGatt`, a desktop BLE stack, a test fake, ...).
 *
 * The peripheral is expected to already know [ServoBleUuids]; callers only deal
 * in raw ASCII payloads.
 */
interface BlePeripheral {
    /** Establish the connection and enable status notifications. */
    suspend fun connect(): Result<Unit>

    /** Write one command payload to the command characteristic. */
    suspend fun write(payload: ByteArray): Result<Unit>

    /** Notifications received from the status characteristic, one line per emit. */
    fun notifications(): Flow<ByteArray>

    /** Emits once per unexpected disconnect that occurs after a successful [connect]. */
    fun connectionLost(): Flow<Unit>

    /** Tear down the connection. */
    suspend fun close()
}
