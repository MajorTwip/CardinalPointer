package com.cardinalpointer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.domain.CameraState

private const val SWIVEL_RANGE = 30f

/** Compass ring: pick a camera's cardinal mount and drag its pointer to set swivel. */
@Composable
fun CompassDial(
    selected: CameraDirection,
    cameras: Map<CameraDirection, CameraState>,
    onSwivelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(220.dp)
            .pointerInput(selected) {
                fun handle(position: Offset) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val bearing = bearingFromPoint(position, center)
                    val delta = shortestAngleDelta(bearing, selected.bearingDegrees)
                        .coerceIn(-SWIVEL_RANGE, SWIVEL_RANGE)
                    onSwivelChange(delta)
                }
                detectDragGestures(
                    onDragStart = { handle(it) },
                    onDrag = { change, _ -> change.consume(); handle(change.position) }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val trackRadius = size.minDimension / 2f * 0.76f
        val tickOuter = trackRadius + size.minDimension * 0.035f
        val anchorRadius = trackRadius + size.minDimension * 0.13f

        drawCircle(ConsoleColors.Line, radius = trackRadius, center = center, style = Stroke(width = 2f))

        var degree = 0
        while (degree < 360) {
            val inner = if (degree % 30 == 0) trackRadius - 7f else trackRadius - 3f
            val p1 = polarOffset(center, inner, degree.toFloat())
            val p2 = polarOffset(center, tickOuter, degree.toFloat())
            drawLine(ConsoleColors.LineSoft, p1, p2, strokeWidth = 2f)
            degree += 10
        }

        val bearing = selected.bearingDegrees
        drawArcBetween(
            center, trackRadius, bearing - SWIVEL_RANGE, bearing + SWIVEL_RANGE,
            ConsoleColors.Accent.copy(alpha = 0.28f), 18f
        )

        CameraDirection.entries.forEach { dir ->
            val isActive = dir == selected
            val p = polarOffset(center, anchorRadius, dir.bearingDegrees)
            drawCircle(
                color = if (isActive) ConsoleColors.Accent else ConsoleColors.InkFaint,
                radius = if (isActive) 5f else 4f,
                center = p
            )
        }

        val camera = cameras.getValue(selected)
        val handlePoint = polarOffset(center, trackRadius, bearing + camera.swivelDegrees)
        drawLine(ConsoleColors.Accent, center, handlePoint, strokeWidth = 2f)
        drawCircle(ConsoleColors.Accent.copy(alpha = 0.16f), radius = 15f, center = handlePoint)
        drawCircle(ConsoleColors.PanelRaised, radius = 8f, center = handlePoint)
        drawCircle(ConsoleColors.Accent, radius = 8f, center = handlePoint, style = Stroke(width = 2.5f))
        drawCircle(ConsoleColors.InkFaint, radius = 3.5f, center = center)
    }
}

/** [fromBearing]/[toBearing] are compass bearings; Compose's arc angle is offset -90° from bearing. */
private fun DrawScope.drawArcBetween(
    center: Offset,
    radius: Float,
    fromBearing: Float,
    toBearing: Float,
    color: Color,
    strokeWidth: Float
) {
    drawArc(
        color = color,
        startAngle = fromBearing - 90f,
        sweepAngle = toBearing - fromBearing,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
