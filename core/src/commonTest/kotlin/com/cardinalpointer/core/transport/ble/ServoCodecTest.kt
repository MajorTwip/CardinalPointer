package com.cardinalpointer.core.transport.ble

import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.transport.Command
import com.cardinalpointer.core.transport.StatusUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServoCodecTest {

    @Test
    fun encodesSwivelAsAzimuth() {
        assertEquals("AZ 12.5", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 12.5f)))
    }

    @Test
    fun encodesDepressionAsElevation() {
        assertEquals("EL -20.0", ServoCodec.encode(Command.SetCameraDepression(CameraDirection.East, -20f)))
    }

    @Test
    fun formatsSubDegreeMagnitudesWithSign() {
        assertEquals("AZ -0.5", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, -0.5f)))
        assertEquals("AZ 0.0", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 0f)))
    }

    @Test
    fun roundsToOneDecimal() {
        assertEquals("AZ 9.9", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 9.94f)))
        assertEquals("AZ 10.0", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 9.96f)))
    }

    @Test
    fun mastCommandsAreUnsupported() {
        assertNull(ServoCodec.encode(Command.SetMastHeight(5f)))
        assertNull(ServoCodec.encode(Command.Erect))
        assertNull(ServoCodec.encode(Command.Fold))
    }

    @Test
    fun decodesPositionLineToBothAxes() {
        val updates = ServoCodec.decodeStatus("POS 120.0 60.0 MOVING", CameraDirection.South)
        assertEquals(
            listOf(
                StatusUpdate.CameraSwivel(CameraDirection.South, 120f),
                StatusUpdate.CameraDepression(CameraDirection.South, 60f),
            ),
            updates,
        )
    }

    @Test
    fun decodesIdlePositionLine() {
        val updates = ServoCodec.decodeStatus("POS 90.0 90.0 IDLE", CameraDirection.North)
        assertEquals(2, updates.size)
    }

    @Test
    fun decodesErrorLine() {
        val updates = ServoCodec.decodeStatus("ERR azimuth clamped", CameraDirection.North)
        assertEquals(listOf(StatusUpdate.Error("azimuth clamped")), updates)
    }

    @Test
    fun ignoresBlankAndUnknownLines() {
        assertTrue(ServoCodec.decodeStatus("   ", CameraDirection.North).isEmpty())
        assertTrue(ServoCodec.decodeStatus("WAT 1 2", CameraDirection.North).isEmpty())
    }

    @Test
    fun toleratesPartialPositionLine() {
        val updates = ServoCodec.decodeStatus("POS 45.0", CameraDirection.West)
        assertEquals(listOf(StatusUpdate.CameraSwivel(CameraDirection.West, 45f)), updates)
    }
}
