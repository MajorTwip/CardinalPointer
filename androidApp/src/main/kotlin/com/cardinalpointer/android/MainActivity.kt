package com.cardinalpointer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    // TODO: create AppViewModel with a real UsbSerialTransport once implemented
    private val viewModel: AppViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CardinalPointerScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun CardinalPointerScreen(viewModel: AppViewModel?) {
    val uiState by viewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CardinalPointer", style = MaterialTheme.typography.headlineMedium)

        if (viewModel == null) {
            Text(
                "No transport connected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        CameraSelector(
            selected = uiState?.selectedCamera ?: CameraDirection.North,
            onSelect = { viewModel?.selectCamera(it) }
        )

        uiState?.let { state ->
            val cam = state.cameras[state.selectedCamera]
            if (cam != null) {
                CameraControls(
                    swivelDegrees = cam.swivelDegrees,
                    depressionDegrees = cam.depressionDegrees,
                    onSwivelChange = viewModel::setSwivel,
                    onDepressionChange = viewModel::setDepression
                )
            }

            MastControls(
                mast = state.mast,
                onHeightChange = viewModel::setTargetHeight,
                onErect = viewModel::erect,
                onFold = viewModel::fold
            )

            state.errorMessage?.let { msg ->
                AlertDialog(
                    onDismissRequest = viewModel::clearError,
                    confirmButton = {
                        TextButton(onClick = viewModel::clearError) { Text("OK") }
                    },
                    title = { Text("Error") },
                    text = { Text(msg) }
                )
            }
        }
    }
}

@Composable
private fun CameraSelector(
    selected: CameraDirection,
    onSelect: (CameraDirection) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Camera", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CameraDirection.entries.forEach { dir ->
                FilterChip(
                    selected = dir == selected,
                    onClick = { onSelect(dir) },
                    label = { Text(dir.name) }
                )
            }
        }
    }
}

@Composable
private fun CameraControls(
    swivelDegrees: Float,
    depressionDegrees: Float,
    onSwivelChange: (Float) -> Unit,
    onDepressionChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Swivel: %.1f°".format(swivelDegrees), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = swivelDegrees,
            onValueChange = onSwivelChange,
            valueRange = -30f..30f
        )
        Text("Depression: %.1f°".format(depressionDegrees), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = depressionDegrees,
            onValueChange = onDepressionChange,
            valueRange = -45f..0f
        )
    }
}

@Composable
private fun MastControls(
    mast: com.cardinalpointer.core.domain.MastState,
    onHeightChange: (Float) -> Unit,
    onErect: () -> Unit,
    onFold: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Mast — current: %.1f m  target: %.1f m  [%s]".format(
                mast.currentHeight, mast.targetHeight, mast.motionState.name
            ),
            style = MaterialTheme.typography.bodySmall
        )
        Slider(
            value = mast.targetHeight,
            onValueChange = onHeightChange,
            valueRange = mast.minHeight..mast.maxHeight
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onErect, modifier = Modifier.weight(1f)) { Text("Erect") }
            OutlinedButton(onClick = onFold, modifier = Modifier.weight(1f)) { Text("Fold") }
        }
    }
}
