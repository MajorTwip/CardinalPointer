package com.cardinalpointer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private val TRACK_HEIGHT = 220.dp

/** Vertical level gauge: filled portion is current height, the bright bar is the draggable target. */
@Composable
fun MastGauge(
    current: Float,
    target: Float,
    min: Float,
    max: Float,
    onTargetChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(34.dp)
            .height(TRACK_HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(ConsoleColors.PanelRaised)
            .border(1.dp, ConsoleColors.Line, RoundedCornerShape(8.dp))
            .pointerInput(min, max) {
                fun handle(y: Float) {
                    val frac = (1f - (y / size.height)).coerceIn(0f, 1f)
                    onTargetChange(min + frac * (max - min))
                }
                detectDragGestures(
                    onDragStart = { handle(it.y) },
                    onDrag = { change, _ -> change.consume(); handle(change.position.y) }
                )
            }
    ) {
        val currentFrac = ((current - min) / (max - min)).coerceIn(0f, 1f)
        val targetFrac = ((target - min) / (max - min)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(TRACK_HEIGHT * currentFrac)
                .background(ConsoleColors.Accent.copy(alpha = 0.35f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = TRACK_HEIGHT * (1f - targetFrac) - 1.5.dp)
                .fillMaxWidth()
                .height(3.dp)
                .background(ConsoleColors.Accent)
        )
    }
}
