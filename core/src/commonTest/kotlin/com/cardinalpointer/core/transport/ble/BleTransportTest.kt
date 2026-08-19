package com.cardinalpointer.core.transport.ble

import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.transport.Command
import com.cardinalpointer.core.transport.StatusUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakePeripheral : BlePeripheral {
    val writes = mutableListOf<String>()
    val incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
    val disconnects = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    var connected = false

    override suspend fun connect(): Result<Unit> {
        connected = true
        return Result.success(Unit)
    }

    override suspend fun write(payload: ByteArray): Result<Unit> {
        writes += payload.decodeToString()
        return Result.success(Unit)
    }

    override fun notifications(): Flow<ByteArray> = incoming

    override fun connectionLost(): Flow<Unit> = disconnects

    override suspend fun close() {
        connected = false
    }
}

class BleTransportTest {

    @Test
    fun sendEncodesAndWritesSupportedCommand() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral)

        val result = transport.send(Command.SetCameraSwivel(CameraDirection.North, 30f))

        assertTrue(result.isSuccess)
        assertEquals(listOf("AZ 0 120.0"), peripheral.writes)
    }

    @Test
    fun sendFailsForUnsupportedCommand() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral)

        val result = transport.send(Command.Erect)

        assertTrue(result.isFailure)
        assertTrue(peripheral.writes.isEmpty())
    }

    @Test
    fun routesCommandsForDifferentCamerasThroughTheSameTransport() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral)

        transport.send(Command.SetCameraSwivel(CameraDirection.North, 0f))
        transport.send(Command.SetCameraSwivel(CameraDirection.West, 0f))

        assertEquals(listOf("AZ 0 90.0", "AZ 3 90.0"), peripheral.writes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun statusNotificationsAreDecodedPerChannel() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral)

        val collected = mutableListOf<StatusUpdate>()
        val job = launch {
            collected += transport.subscribeStatusUpdates().take(3).toList()
        }
        runCurrent() // let the collector subscribe before we emit (replay = 0)

        peripheral.incoming.emit("POS 1 120.0 60.0 MOVING".encodeToByteArray())
        peripheral.incoming.emit("ERR out of range".encodeToByteArray())
        job.join()

        assertEquals(
            listOf(
                StatusUpdate.CameraSwivel(CameraDirection.East, 30f),
                StatusUpdate.CameraDepression(CameraDirection.East, -30f),
                StatusUpdate.Error("out of range"),
            ),
            collected,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun statusNotificationsForDifferentChannelsRouteToDifferentCameras() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral)

        val collected = mutableListOf<StatusUpdate>()
        val job = launch {
            collected += transport.subscribeStatusUpdates().take(4).toList()
        }
        runCurrent()

        peripheral.incoming.emit("POS 0 90.0 90.0 IDLE".encodeToByteArray())
        peripheral.incoming.emit("POS 2 90.0 90.0 IDLE".encodeToByteArray())
        job.join()

        assertEquals(
            listOf(
                StatusUpdate.CameraSwivel(CameraDirection.North, 0f),
                StatusUpdate.CameraDepression(CameraDirection.North, 0f),
                StatusUpdate.CameraSwivel(CameraDirection.South, 0f),
                StatusUpdate.CameraDepression(CameraDirection.South, 0f),
            ),
            collected,
        )
    }

    @Test
    fun connectAndDisconnectDelegateToPeripheral() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral)

        transport.connect()
        assertTrue(peripheral.connected)

        transport.disconnect()
        assertTrue(!peripheral.connected)
    }
}
