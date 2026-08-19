package com.cardinalpointer.android.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.cardinalpointer.core.transport.ble.ServoBleUuids
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Scans for the one ESP32 servo device the app knows how to talk to,
 * filtering on the GATT service it advertises (see [ServoBleUuids]).
 */
@SuppressLint("MissingPermission")
class BleServoScanner(private val context: Context) {

    /** Returns the first matching device, or `null` if none was found within [timeoutMs]. */
    suspend fun scanForServo(timeoutMs: Long = DEFAULT_TIMEOUT_MS): BluetoothDevice? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter?.takeIf { it.isEnabled } ?: return null
        val scanner = adapter.bluetoothLeScanner ?: return null

        return withTimeoutOrNull(timeoutMs) {
            callbackFlow {
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        trySend(result.device)
                    }

                    override fun onScanFailed(errorCode: Int) {
                        close(IllegalStateException("BLE scan failed: $errorCode"))
                    }
                }

                val filters = listOf(
                    ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(UUID.fromString(ServoBleUuids.SERVICE)))
                        .build()
                )
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                scanner.startScan(filters, settings, callback)
                awaitClose { scanner.stopScan(callback) }
            }.first()
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 12_000L
    }
}
