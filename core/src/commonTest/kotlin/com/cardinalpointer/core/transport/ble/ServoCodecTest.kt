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
    fun encodesSwivelAsAzimuthWithChannelAndHomeOffset() {
        assertEquals("AZ 0 102.5", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 12.5f)))
    }

    @Test
    fun encodesDepressionAsElevationWithChannelAndHomeOffset() {
        assertEquals("EL 1 70.0", ServoCodec.encode(Command.SetCameraDepression(CameraDirection.East, -20f)))
    }

    @Test
    fun encodesDistinctChannelPerCameraDirection() {
        assertEquals("AZ 0 90.0", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 0f)))
        assertEquals("AZ 1 90.0", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.East, 0f)))
        assertEquals("AZ 2 90.0", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.South, 0f)))
        assertEquals("AZ 3 90.0", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.West, 0f)))
    }

    @Test
    fun formatsSubDegreeMagnitudesWithSign() {
        assertEquals("AZ 0 89.5", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, -0.5f)))
        assertEquals("AZ 0 90.0", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 0f)))
    }

    @Test
    fun roundsToOneDecimal() {
        assertEquals("AZ 0 99.9", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 9.94f)))
        assertEquals("AZ 0 100.0", ServoCodec.encode(Command.SetCameraSwivel(CameraDirection.North, 9.96f)))
    }

    @Test
    fun mastCommandsAreUnsupported() {
        assertNull(ServoCodec.encode(Command.SetMastHeight(5f)))
        assertNull(ServoCodec.encode(Command.Erect))
        assertNull(ServoCodec.encode(Command.Fold))
    }

    @Test
    fun decodesPositionLineToBothAxesRelativeToHome() {
        val updates = ServoCodec.decodeStatus("POS 2 120.0 60.0 MOVING")
        assertEquals(
            listOf(
                StatusUpdate.CameraSwivel(CameraDirection.South, 30f),
                StatusUpdate.CameraDepression(CameraDirection.South, -30f),
            ),
            updates,
        )
    }

    @Test
    fun decodesIdlePositionLine() {
        val updates = ServoCodec.decodeStatus("POS 0 90.0 90.0 IDLE")
        assertEquals(2, updates.size)
    }

    @Test
    fun decodesEachChannelToItsOwnCameraDirection() {
        assertEquals(
            CameraDirection.North,
            (ServoCodec.decodeStatus("POS 0 90.0 90.0 IDLE").first() as StatusUpdate.CameraSwivel).direction,
        )
        assertEquals(
            CameraDirection.West,
            (ServoCodec.decodeStatus("POS 3 90.0 90.0 IDLE").first() as StatusUpdate.CameraSwivel).direction,
        )
    }

    @Test
    fun decodesErrorLineWithoutChannel() {
        val updates = ServoCodec.decodeStatus("ERR unknown command: XYZ")
        assertEquals(listOf(StatusUpdate.Error("unknown command: XYZ")), updates)
    }

    @Test
    fun decodesErrorLineWithChannelPrefix() {
        val updates = ServoCodec.decodeStatus("ERR 2 azimuth clamped")
        assertEquals(listOf(StatusUpdate.Error("cam 2: azimuth clamped")), updates)
    }

    @Test
    fun ignoresBlankAndUnknownLines() {
        assertTrue(ServoCodec.decodeStatus("   ").isEmpty())
        assertTrue(ServoCodec.decodeStatus("WAT 1 2").isEmpty())
    }

    @Test
    fun ignoresPositionLinesWithMissingOrOutOfRangeChannel() {
        assertTrue(ServoCodec.decodeStatus("POS 9 120.0 60.0 MOVING").isEmpty())
        assertTrue(ServoCodec.decodeStatus("POS abc 120.0 60.0 MOVING").isEmpty())
    }

    @Test
    fun toleratesPartialPositionLine() {
        val updates = ServoCodec.decodeStatus("POS 3 45.0")
        assertEquals(listOf(StatusUpdate.CameraSwivel(CameraDirection.West, -45f)), updates)
    }
}
