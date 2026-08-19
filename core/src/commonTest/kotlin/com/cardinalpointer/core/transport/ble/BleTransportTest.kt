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

    override suspend fun close() {
        connected = false
    }
}

class BleTransportTest {

    @Test
    fun sendEncodesAndWritesSupportedCommand() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral, CameraDirection.North)

        val result = transport.send(Command.SetCameraSwivel(CameraDirection.North, 30f))

        assertTrue(result.isSuccess)
        assertEquals(listOf("AZ 30.0"), peripheral.writes)
    }

    @Test
    fun sendFailsForUnsupportedCommand() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral, CameraDirection.North)

        val result = transport.send(Command.Erect)

        assertTrue(result.isFailure)
        assertTrue(peripheral.writes.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun statusNotificationsAreDecodedForConfiguredDirection() = runTest {
        val peripheral = FakePeripheral()
        val transport = BleTransport(peripheral, CameraDirection.East)

        val collected = mutableListOf<StatusUpdate>()
        val job = launch {
            collected += transport.subscribeStatusUpdates().take(3).toList()
        }
        runCurrent() // let the collector subscribe before we emit (replay = 0)

        peripheral.incoming.emit("POS 120.0 60.0 MOVING".encodeToByteArray())
        peripheral.incoming.emit("ERR out of range".encodeToByteArray())
        job.join()

        assertEquals(
            listOf(
                StatusUpdate.CameraSwivel(CameraDirection.East, 120f),
                StatusUpdate.CameraDepression(CameraDirection.East, 60f),
                StatusUpdate.Error("out of range"),
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
