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
 * The firmware drives 4 independent two-axis aimers (one per camera), so every
 * command/status line carries a leading camera-channel argument (0-3), mapped
 * to/from [CameraDirection] via [CameraDirection.entries] order (North=0,
 * East=1, South=2, West=3 — must stay in sync with the firmware's camera
 * array order):
 *  - [Command.SetCameraSwivel]     -> `AZ <cam> <deg>`   (azimuth)
 *  - [Command.SetCameraDepression] -> `EL <cam> <deg>`   (elevation)
 *
 * The app's swivel/depression are offsets from center (swivel: -30..30,
 * depression: -45..0), but the firmware's `AZ`/`EL` arguments are *absolute*
 * servo angles in 0..180 with home at 90 (see `firmware/src/config.h`'s
 * `AZIMUTH_HOME_DEG`/`ELEVATION_HOME_DEG`). [HOME_DEG] converts between the
 * two so an app-side offset never goes negative and gets silently clamped by
 * the firmware.
 *
 * Mast-oriented commands ([Command.SetMastHeight], [Command.Erect],
 * [Command.Fold]) have no meaning for this device and [encode] returns `null`.
 */
object ServoCodec {

    /** Encode a command to its wire line, or `null` if the device can't do it. */
    fun encode(command: Command): String? = when (command) {
        is Command.SetCameraSwivel ->
            "AZ ${channelOf(command.direction)} ${formatDeg(HOME_DEG + command.degrees)}"
        is Command.SetCameraDepression ->
            "EL ${channelOf(command.direction)} ${formatDeg(HOME_DEG + command.degrees)}"
        is Command.SetMastHeight -> null
        Command.Erect -> null
        Command.Fold -> null
    }

    /**
     * Parse one status line into zero or more [StatusUpdate]s.
     *
     *  - `POS <cam> <az> <el> <MOVING|IDLE>` -> a swivel and a depression update for
     *    the [CameraDirection] at index `<cam>`
     *  - `ERR <cam> <text>` or `ERR <text>`  -> an [StatusUpdate.Error], camera-prefixed
     *    when a channel is present (parse-level firmware errors have none)
     *
     * Unknown, malformed, or out-of-range-channel lines yield an empty list.
     */
    fun decodeStatus(line: String): List<StatusUpdate> {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return emptyList()

        val parts = trimmed.split(WHITESPACE)
        return when (parts[0].uppercase()) {
            "POS" -> {
                val direction = parts.getOrNull(1)?.toIntOrNull()?.let(::directionForChannel)
                    ?: return emptyList()
                buildList {
                    parts.getOrNull(2)?.toFloatOrNull()?.let {
                        add(StatusUpdate.CameraSwivel(direction, it - HOME_DEG))
                    }
                    parts.getOrNull(3)?.toFloatOrNull()?.let {
                        add(StatusUpdate.CameraDepression(direction, it - HOME_DEG))
                    }
                }
            }
            "ERR" -> {
                val rest = trimmed.substring(parts[0].length).trim()
                val channel = parts.getOrNull(1)?.toIntOrNull()
                val message = if (channel != null) rest.removePrefix(channel.toString()).trim() else rest
                listOf(StatusUpdate.Error(if (channel != null) "cam $channel: $message" else message))
            }
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

    private fun channelOf(direction: CameraDirection): Int = CameraDirection.entries.indexOf(direction)

    private fun directionForChannel(channel: Int): CameraDirection? = CameraDirection.entries.getOrNull(channel)

    private val WHITESPACE = Regex("\\s+")

    /** Matches both `AZIMUTH_HOME_DEG` and `ELEVATION_HOME_DEG` in `firmware/src/config.h`. */
    private const val HOME_DEG = 90f
}
