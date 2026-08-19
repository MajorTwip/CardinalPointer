package com.cardinalpointer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEPRESSION_MIN = -45f
private const val DEPRESSION_MAX = 0f

/** Quarter-arc tilt gauge: 0° (level, pointing right) sweeping down to -45°. */
@Composable
fun ElevationGauge(
    depressionDegrees: Float,
    onDepressionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDepression by rememberUpdatedState(depressionDegrees)
    val currentOnDepressionChange by rememberUpdatedState(onDepressionChange)

    Canvas(
        modifier = modifier
            .size(190.dp)
            .pointerInput(Unit) {
                val pivot = Offset(size.width * 0.18f, size.height * 0.18f)
                fun phiOf(position: Offset): Float {
                    val dx = position.x - pivot.x
                    val dy = position.y - pivot.y
                    return atan2(dy, dx) * 180f / PI.toFloat()
                }
                var lastPhi = 0f
                var value = 0f
                detectDragGestures(
                    onDragStart = { position ->
                        lastPhi = phiOf(position)
                        value = currentDepression
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val phi = phiOf(change.position)
                        value = (value - shortestAngleDelta(phi, lastPhi))
                            .coerceIn(DEPRESSION_MIN, DEPRESSION_MAX)
                        lastPhi = phi
                        currentOnDepressionChange(value)
                    }
                )
            }
    ) {
        val pivot = Offset(size.width * 0.18f, size.height * 0.18f)
        val radius = size.minDimension * 0.62f

        fun pointForDepression(depression: Float): Offset {
            val phi = -depression * PI.toFloat() / 180f
            return Offset(pivot.x + radius * cos(phi), pivot.y + radius * sin(phi))
        }

        drawArc(
            color = ConsoleColors.Line,
            startAngle = 0f,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = Offset(pivot.x - radius, pivot.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 2f)
        )

        listOf(0f, -15f, -30f, -45f).forEach { tick ->
            val p1 = pointForDepression(tick)
            val dx = p1.x - pivot.x
            val dy = p1.y - pivot.y
            val len = sqrt(dx * dx + dy * dy)
            val ux = dx / len
            val uy = dy / len
            val p2 = Offset(pivot.x + (radius + 9f) * ux, pivot.y + (radius + 9f) * uy)
            drawLine(ConsoleColors.InkFaint, p1, p2, strokeWidth = 2f)
        }

        val handlePoint = pointForDepression(depressionDegrees)
        drawLine(ConsoleColors.Accent, pivot, handlePoint, strokeWidth = 2f)
        drawCircle(ConsoleColors.Accent.copy(alpha = 0.16f), radius = 15f, center = handlePoint)
        drawCircle(ConsoleColors.PanelRaised, radius = 8f, center = handlePoint)
        drawCircle(ConsoleColors.Accent, radius = 8f, center = handlePoint, style = Stroke(width = 2.5f))
        drawCircle(ConsoleColors.InkFaint, radius = 3.5f, center = pivot)
    }
}
