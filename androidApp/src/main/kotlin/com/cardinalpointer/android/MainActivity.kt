package com.cardinalpointer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.cardinalpointer.core.transport.FakeTransport
import com.cardinalpointer.core.viewmodel.AppViewModel
import com.cardinalpointer.ui.CardinalPointerScreen
import com.cardinalpointer.ui.ConsoleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            // TODO: swap FakeTransport for a real UsbSerialTransport once implemented.
            val viewModel = remember { AppViewModel(FakeTransport(scope), scope) }
            ConsoleTheme {
                CardinalPointerScreen(viewModel)
            }
        }
    }
}
