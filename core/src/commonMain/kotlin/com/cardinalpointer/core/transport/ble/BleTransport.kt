package com.cardinalpointer.core.transport.ble

import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.transport.Command
import com.cardinalpointer.core.transport.StatusUpdate
import com.cardinalpointer.core.transport.Transport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/**
 * [Transport] backed by the ESP32 servo interface over BLE.
 *
 * Composes a platform [BlePeripheral] with [ServoCodec]: commands are encoded to
 * ASCII lines and written to the command characteristic; status notifications
 * are parsed back into [StatusUpdate]s.
 *
 * Because the firmware is a single two-axis aimer, one transport instance maps
 * to one [direction] (which camera/device this ESP32 points).
 *
 * @param peripheral platform BLE connection to the servo device
 * @param direction  camera slot this device aims (default [CameraDirection.North])
 */
class BleTransport(
    private val peripheral: BlePeripheral,
    private val direction: CameraDirection = CameraDirection.North,
) : Transport {

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
            ServoCodec.decodeStatus(bytes.decodeToString(), direction)
                .forEach { emit(it) }
        }
}
