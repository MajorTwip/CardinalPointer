package com.cardinalpointer.core.transport.ble

import com.cardinalpointer.core.transport.Command
import com.cardinalpointer.core.transport.StatusUpdate
import com.cardinalpointer.core.transport.Transport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/**
 * [Transport] backed by the ESP32 servo interface over BLE.
 *
 * Composes a platform [BlePeripheral] with [ServoCodec]: commands are encoded to
 * ASCII lines (each carrying a camera-channel argument) and written to the
 * command characteristic; status notifications are parsed back into
 * [StatusUpdate]s already attributed to the right camera by [ServoCodec]. One
 * transport instance (one BLE connection) serves all 4 cameras on the ESP32.
 *
 * @param peripheral platform BLE connection to the servo device
 */
class BleTransport(private val peripheral: BlePeripheral) : Transport {

    /** Connect to the peripheral. Call once before sending commands. */
    suspend fun connect(): Result<Unit> = peripheral.connect()

    /** Disconnect from the peripheral. */
    suspend fun disconnect() = peripheral.close()

    override suspend fun send(command: Command): Result<Unit> {
        val line = ServoCodec.encode(command)
            ?: return Result.failure(
                UnsupportedOperationException(
                    "Command $command is not supported by the servo device"
                )
            )
        return peripheral.write(line.encodeToByteArray())
    }

    override fun subscribeStatusUpdates(): Flow<StatusUpdate> =
        peripheral.notifications().transform { bytes ->
            ServoCodec.decodeStatus(bytes.decodeToString())
                .forEach { emit(it) }
        }
}
