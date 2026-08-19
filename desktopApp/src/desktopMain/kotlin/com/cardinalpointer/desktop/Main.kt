package com.cardinalpointer.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cardinalpointer.core.transport.FakeTransport
import com.cardinalpointer.core.viewmodel.AppViewModel
import com.cardinalpointer.ui.CardinalPointerScreen
import com.cardinalpointer.ui.ConsoleTheme
import com.cardinalpointer.ui.LinkStatus

fun main() = application {
    val windowState = rememberWindowState(width = 1180.dp, height = 820.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "CardinalPointer",
        state = windowState
    ) {
        val scope = rememberCoroutineScope()
        // No real transport on desktop yet (BleTransport is Android-only); always simulated.
        val viewModel = remember { AppViewModel(FakeTransport(scope), scope) }
        ConsoleTheme {
            CardinalPointerScreen(viewModel, linkStatus = LinkStatus.SimulatedOnly)
        }
    }
}
