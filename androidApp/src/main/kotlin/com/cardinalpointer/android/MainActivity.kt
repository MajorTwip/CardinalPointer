package com.cardinalpointer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.cardinalpointer.android.ble.BleConnectionCoordinator
import com.cardinalpointer.core.transport.FakeTransport
import com.cardinalpointer.core.viewmodel.AppViewModel
import com.cardinalpointer.ui.CardinalPointerScreen
import com.cardinalpointer.ui.ConsoleTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var coordinator: BleConnectionCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            coordinator.onPermissionResult(grants.values.all { it })
        }
        coordinator = BleConnectionCoordinator(
            context = applicationContext,
            scope = coordinatorScope,
            requestPermissions = { perms -> permissionLauncher.launch(perms) },
        )
        coordinator.start()

        setContent {
            val outerScope = rememberCoroutineScope()
            // BleConnectionCoordinator resolves a live BleTransport once the ESP32 servo is
            // found and connected; FakeTransport remains the always-available simulated
            // fallback (see LinkStatus) while scanning/connecting or on failure.
            val fakeTransport = remember { FakeTransport(outerScope) }

            val linkStatus by coordinator.status.collectAsState()
            val liveTransport by coordinator.transport.collectAsState()
            val activeTransport = liveTransport ?: fakeTransport

            // Each AppViewModel subscribes to its transport's status flow for its whole
            // lifetime, so give every transport swap its own cancelable scope instead of
            // leaking the previous instance's collector into outerScope forever.
            val viewModelScope = remember(activeTransport) {
                CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            }
            DisposableEffect(activeTransport) {
                onDispose { viewModelScope.cancel() }
            }
            val viewModel = remember(activeTransport) { AppViewModel(activeTransport, viewModelScope) }

            ConsoleTheme {
                CardinalPointerScreen(
                    viewModel = viewModel,
                    linkStatus = linkStatus,
                    onRetryLink = coordinator::retry,
                )
            }
        }
    }

    override fun onDestroy() {
        coordinator.dispose()
        coordinatorScope.cancel()
        super.onDestroy()
    }
}
