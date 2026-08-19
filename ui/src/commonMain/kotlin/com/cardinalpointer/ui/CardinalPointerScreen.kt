package com.cardinalpointer.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.domain.CameraState
import com.cardinalpointer.core.domain.MastState
import com.cardinalpointer.core.domain.MotionState
import com.cardinalpointer.core.viewmodel.AppViewModel

@Composable
fun CardinalPointerScreen(
    viewModel: AppViewModel,
    linkStatus: LinkStatus = LinkStatus.SimulatedOnly,
    onRetryLink: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConsoleColors.Bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConsoleHeader(linkStatus, onRetryLink)
        AimPanel(
            selected = uiState.selectedCamera,
            cameras = uiState.cameras,
            onSelect = viewModel::selectCamera,
            onSwivelChange = viewModel::setSwivel,
            onDepressionChange = viewModel::setDepression
        )
        MastPanel(
            mast = uiState.mast,
            onTargetChange = viewModel::setTargetHeight,
            onErect = viewModel::erect,
            onFold = viewModel::fold
        )
        Text(
            "Drag the pointers to aim. Dummy transport — not wired to hardware yet.",
            color = ConsoleColors.InkFaint,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }

    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            title = { Text("Error") },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun ConsoleHeader(linkStatus: LinkStatus, onRetryLink: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ConsoleColors.Panel)
            .border(1.dp, ConsoleColors.Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CardinalPointer", color = ConsoleColors.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("AIM CONSOLE", color = ConsoleColors.InkFaint, fontSize = 11.sp, letterSpacing = 1.5.sp)
        }
        LinkStatusBadge(linkStatus, onRetryLink)
    }
}

@Composable
private fun LinkStatusBadge(status: LinkStatus, onRetry: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "link")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse),
        label = "linkAlpha"
    )
    val dotColor = when {
        status.isLive -> ConsoleColors.Accent
        status.isRetryable -> ConsoleColors.Red
        else -> ConsoleColors.Amber
    }
    Row(
        modifier = Modifier
            .then(if (status.isRetryable) Modifier.clickable(onClick = onRetry) else Modifier)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = if (status.isLive) 1f else pulse))
        )
        Text(
            status.label,
            color = ConsoleColors.InkDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StateChip(state: MotionState) {
    val color = when (state) {
        MotionState.Idle -> ConsoleColors.InkDim
        MotionState.Error -> ConsoleColors.Red
        MotionState.Erecting, MotionState.Folding -> ConsoleColors.Amber
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(state.name.uppercase(), color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun AimPanel(
    selected: CameraDirection,
    cameras: Map<CameraDirection, CameraState>,
    onSelect: (CameraDirection) -> Unit,
    onSwivelChange: (Float) -> Unit,
    onDepressionChange: (Float) -> Unit
) {
    val camera = cameras.getValue(selected)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, ConsoleColors.Line, RoundedCornerShape(10.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AIM — ${selected.name.uppercase()}", color = ConsoleColors.InkFaint, fontSize = 12.sp, letterSpacing = 1.sp)
            CameraTabs(selected, onSelect)
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 480.dp
            val dials = @Composable {
                DialColumn("SWIVEL", "${swivelLabel(camera.swivelDegrees)}  ·  range ±30°") {
                    CompassDial(selected, cameras, onSwivelChange)
                }
                DialColumn("DEPRESSION", "${depressionLabel(camera.depressionDegrees)}  ·  range 0° to −45°") {
                    ElevationGauge(camera.depressionDegrees, onDepressionChange)
                }
            }
            if (stacked) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) { dials() }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) { dials() }
            }
        }
    }
}

@Composable
private fun DialColumn(label: String, caption: String, dial: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        dial()
        Text("$label $caption", color = ConsoleColors.InkDim, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun CameraTabs(selected: CameraDirection, onSelect: (CameraDirection) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CameraDirection.entries.forEach { dir ->
            val isActive = dir == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isActive) ConsoleColors.Accent else ConsoleColors.Panel)
                    .border(1.dp, if (isActive) ConsoleColors.Accent else ConsoleColors.Line, RoundedCornerShape(6.dp))
                    .clickable { onSelect(dir) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    dir.name.take(1),
                    color = if (isActive) ConsoleColors.AccentInk else ConsoleColors.InkDim,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MastPanel(
    mast: MastState,
    onTargetChange: (Float) -> Unit,
    onErect: () -> Unit,
    onFold: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ConsoleColors.Panel)
            .border(1.dp, ConsoleColors.Line, RoundedCornerShape(10.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MAST", color = ConsoleColors.InkFaint, fontSize = 12.sp, letterSpacing = 1.sp)
            StateChip(mast.motionState)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("CURRENT", color = ConsoleColors.InkFaint, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(heightLabel(mast.currentHeight), color = ConsoleColors.Ink, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
                }
                Column {
                    Text("TARGET", color = ConsoleColors.InkFaint, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text(heightLabel(mast.targetHeight), color = ConsoleColors.Accent, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
                }
            }
            HeightStepper(
                target = mast.targetHeight,
                min = mast.minHeight,
                max = mast.maxHeight,
                onTargetChange = onTargetChange
            )
        }

        val isBusy = mast.motionState == MotionState.Erecting || mast.motionState == MotionState.Folding
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onErect,
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ConsoleColors.Accent, contentColor = ConsoleColors.AccentInk)
            ) { Text("ERECT") }
            OutlinedButton(onClick = onFold, enabled = !isBusy, modifier = Modifier.weight(1f)) { Text("FOLD") }
        }
    }
}

private const val MAST_STEP_METERS = 0.5f

@Composable
private fun HeightStepper(target: Float, min: Float, max: Float, onTargetChange: (Float) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepperButton("−") { onTargetChange((target - MAST_STEP_METERS).coerceIn(min, max)) }
        StepperButton("+") { onTargetChange((target + MAST_STEP_METERS).coerceIn(min, max)) }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ConsoleColors.PanelRaised)
            .border(1.dp, ConsoleColors.Line, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = ConsoleColors.Ink,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}
