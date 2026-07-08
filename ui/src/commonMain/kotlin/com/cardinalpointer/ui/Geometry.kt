package com.cardinalpointer.ui

import androidx.compose.ui.geometry.Offset
import com.cardinalpointer.core.domain.CameraDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/** Compass bearing in degrees, 0 = North (up), clockwise positive. */
val CameraDirection.bearingDegrees: Float
    get() = when (this) {
        CameraDirection.North -> 0f
        CameraDirection.East -> 90f
        CameraDirection.South -> 180f
        CameraDirection.West -> 270f
    }

/** Point on a circle of [radius] around [center] at compass [bearingDegrees]. */
fun polarOffset(center: Offset, radius: Float, bearingDegrees: Float): Offset {
    val rad = bearingDegrees * PI.toFloat() / 180f
    return Offset(center.x + radius * sin(rad), center.y - radius * cos(rad))
}

/** Compass bearing of [point] as seen from [center]. */
fun bearingFromPoint(point: Offset, center: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val deg = atan2(dx, -dy) * 180f / PI.toFloat()
    return (deg + 360f) % 360f
}

/** Shortest signed angular difference `a - b`, wrapped to [-180, 180]. */
fun shortestAngleDelta(a: Float, b: Float): Float = ((a - b + 540f) % 360f) - 180f

private fun splitDecimal(value: Float): Triple<String, Int, Int> {
    val rounded = round(value * 10f) / 10f
    val whole = rounded.toInt()
    val tenths = round(abs(rounded - whole) * 10f).toInt()
    val sign = if (rounded < 0f) "-" else ""
    return Triple(sign, abs(whole), tenths)
}

/** e.g. "+6.0°" / "-18.0°" / "+0.0°" — matches the sign convention used for swivel. */
fun swivelLabel(value: Float): String {
    val (_, whole, tenths) = splitDecimal(value)
    val sign = if (value >= 0f) "+" else "-"
    return "$sign$whole.$tenths°"
}

/** e.g. "-18.0°" / "0.0°" — depression is always zero or negative. */
fun depressionLabel(value: Float): String {
    val (sign, whole, tenths) = splitDecimal(value)
    return "$sign$whole.$tenths°"
}

/** e.g. "7.5 m" */
fun heightLabel(value: Float): String {
    val (sign, whole, tenths) = splitDecimal(value)
    return "$sign$whole.$tenths m"
}
