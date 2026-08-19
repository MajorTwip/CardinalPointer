package com.cardinalpointer.core.transport.ble

import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.transport.Command
import com.cardinalpointer.core.transport.StatusUpdate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Translates between the core [Command]/[StatusUpdate] contract and the ASCII
 * line protocol spoken by the ESP32 servo firmware (see `firmware/README.md`).
 *
 * The firmware drives a single two-axis aimer (azimuth + elevation), so it maps
 * to exactly one [CameraDirection]:
 *  - [Command.SetCameraSwivel]     -> `AZ <deg>`   (azimuth)
 *  - [Command.SetCameraDepression] -> `EL <deg>`   (elevation)
 *
 * Mast-oriented commands ([Command.SetMastHeight], [Command.Erect],
 * [Command.Fold]) have no meaning for this device and [encode] returns `null`.
 */
object ServoCodec {

    /** Encode a command to its wire line, or `null` if the device can't do it. */
    fun encode(command: Command): String? = when (command) {
        is Command.SetCameraSwivel -> "AZ ${formatDeg(command.degrees)}"
        is Command.SetCameraDepression -> "EL ${formatDeg(command.degrees)}"
        is Command.SetMastHeight -> null
        Command.Erect -> null
        Command.Fold -> null
    }

    /**
     * Parse one status line into zero or more [StatusUpdate]s for [direction].
     *
     *  - `POS <az> <el> <MOVING|IDLE>` -> a swivel and a depression update
     *  - `ERR <text>`                  -> an [StatusUpdate.Error]
     *
     * Unknown or malformed lines yield an empty list.
     */
    fun decodeStatus(line: String, direction: CameraDirection): List<StatusUpdate> {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return emptyList()

        val parts = trimmed.split(WHITESPACE)
        return when (parts[0].uppercase()) {
            "POS" -> buildList {
                parts.getOrNull(1)?.toFloatOrNull()?.let {
                    add(StatusUpdate.CameraSwivel(direction, it))
                }
                parts.getOrNull(2)?.toFloatOrNull()?.let {
                    add(StatusUpdate.CameraDepression(direction, it))
                }
            }
            "ERR" -> listOf(StatusUpdate.Error(trimmed.substring(parts[0].length).trim()))
            else -> emptyList()
        }
    }

    /** Format a degree value with a single decimal, e.g. `12.5`, `-0.5`. */
    private fun formatDeg(value: Float): String {
        val scaled = (value * 10f).roundToInt()
        val magnitude = abs(scaled)
        val sign = if (scaled < 0) "-" else ""
        return "$sign${magnitude / 10}.${magnitude % 10}"
    }

    private val WHITESPACE = Regex("\\s+")
}
