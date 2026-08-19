package com.cardinalpointer.android.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.cardinalpointer.core.transport.Transport
import com.cardinalpointer.core.transport.ble.BleTransport
import com.cardinalpointer.core.transport.ble.BlePeripheral
import com.cardinalpointer.ui.LinkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns the whole auto-connect lifecycle for the ESP32 servo: permission
 * check/request, scan, connect, and disconnect detection. There is only ever
 * one known device, so this drives a single automatic attempt rather than
 * exposing a device picker.
 */
class BleConnectionCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val requestPermissions: (Array<String>) -> Unit,
) {
    private val _status = MutableStateFlow<LinkStatus>(LinkStatus.Idle)
    val status: StateFlow<LinkStatus> = _status.asStateFlow()

    private val _transport = MutableStateFlow<Transport?>(null)
    val transport: StateFlow<Transport?> = _transport.asStateFlow()

    private val scanner = BleServoScanner(context)

    private var job: Job? = null
    private var activePeripheral: BlePeripheral? = null

    /** Kick off the first attempt. Safe to call once, e.g. from `onCreate`. */
    fun start() = runFlow()

    /** Re-checks permissions and restarts scan/connect from scratch. */
    fun retry() = runFlow()

    /** Feed the result of the runtime permission request here. */
    fun onPermissionResult(granted: Boolean) {
        if (granted) proceedToScan() else _status.value = LinkStatus.PermissionDenied()
    }

    /** Tear down any in-flight or active connection. Call from `onDestroy`. */
    fun dispose() {
        job?.cancel()
        job = null
        teardownActiveConnection()
    }

    private fun runFlow() {
        job?.cancel()
        teardownActiveConnection()
        job = scope.launch {
            val missing = requiredRuntimePermissions().filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) {
                proceedToScan()
            } else {
                _status.value = LinkStatus.RequestingPermission
                requestPermissions(missing.toTypedArray())
            }
        }
    }

    private fun proceedToScan() {
        job = scope.launch {
            _status.value = LinkStatus.Scanning
            val device = scanner.scanForServo()
            if (device == null) {
                _status.value = LinkStatus.DeviceNotFound()
            } else {
                connect(device)
            }
        }
    }

    private suspend fun connect(device: BluetoothDevice) {
        _status.value = LinkStatus.Connecting
        val peripheral = AndroidBlePeripheral(context, device)
        val bleTransport = BleTransport(peripheral)
        bleTransport.connect()
            .onSuccess {
                activePeripheral = peripheral
                _transport.value = bleTransport
                _status.value = LinkStatus.Connected
                scope.launch {
                    peripheral.connectionLost().first()
                    teardownActiveConnection()
                    _status.value = LinkStatus.Disconnected
                }
            }
            .onFailure { error ->
                peripheral.close()
                _status.value = LinkStatus.ConnectFailed(error.message)
            }
    }

    private fun teardownActiveConnection() {
        _transport.value = null
        val peripheral = activePeripheral
        activePeripheral = null
        if (peripheral != null) scope.launch { peripheral.close() }
    }

    private fun requiredRuntimePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }
}
