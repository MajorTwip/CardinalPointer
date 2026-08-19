package com.cardinalpointer.android.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.cardinalpointer.core.transport.ble.BlePeripheral
import com.cardinalpointer.core.transport.ble.ServoBleUuids
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

/**
 * Android [BlePeripheral] backed by [BluetoothGatt].
 *
 * Bridges the callback-based GATT API onto the suspend/[Flow] contract used by
 * [com.cardinalpointer.core.transport.ble.BleTransport]. Construct it with a
 * [BluetoothDevice] you have already discovered (via scan) or bonded.
 *
 * The caller is responsible for holding the `BLUETOOTH_CONNECT` runtime
 * permission before invoking [connect]; methods are annotated
 * `@SuppressLint("MissingPermission")` on that basis.
 *
 * NOTE: this file targets the Android SDK and was not compiled in the
 * environment that generated it (no Android SDK available there).
 */
@SuppressLint("MissingPermission")
class AndroidBlePeripheral(
    private val context: Context,
    private val device: BluetoothDevice,
) : BlePeripheral {

    private val incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

    private var gatt: BluetoothGatt? = null
    private var commandChar: BluetoothGattCharacteristic? = null
    private var pendingConnect: CompletableDeferred<Result<Unit>>? = null

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
                BluetoothProfile.STATE_DISCONNECTED -> completeConnect(
                    Result.failure(IllegalStateException("Disconnected (status=$status)"))
                )
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                completeConnect(Result.failure(IllegalStateException("Service discovery failed: $status")))
                return
            }
            val service = g.getService(SERVICE_UUID)
            val cmd = service?.getCharacteristic(COMMAND_UUID)
            val statusChar = service?.getCharacteristic(STATUS_UUID)
            if (cmd == null || statusChar == null) {
                completeConnect(Result.failure(IllegalStateException("Servo GATT profile not found")))
                return
            }
            commandChar = cmd
            enableNotifications(g, statusChar)
            completeConnect(Result.success(Unit))
        }

        // API 33+
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            ch: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (ch.uuid == STATUS_UUID) incoming.tryEmit(value)
        }

        // Pre-33
        @Deprecated("Deprecated in Android 13")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            if (ch.uuid == STATUS_UUID) {
                @Suppress("DEPRECATION")
                ch.value?.let { incoming.tryEmit(it) }
            }
        }
    }

    override suspend fun connect(): Result<Unit> {
        val deferred = CompletableDeferred<Result<Unit>>()
        pendingConnect = deferred
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        return deferred.await()
    }

    override suspend fun write(payload: ByteArray): Result<Unit> {
        val g = gatt ?: return Result.failure(IllegalStateException("Not connected"))
        val ch = commandChar ?: return Result.failure(IllegalStateException("Command characteristic unavailable"))
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val rc = g.writeCharacteristic(ch, payload, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                if (rc == BluetoothGatt.GATT_SUCCESS) Result.success(Unit)
                else Result.failure(IllegalStateException("writeCharacteristic rc=$rc"))
            } else {
                @Suppress("DEPRECATION")
                run {
                    ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    ch.value = payload
                    if (g.writeCharacteristic(ch)) Result.success(Unit)
                    else Result.failure(IllegalStateException("writeCharacteristic failed"))
                }
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }

    override fun notifications(): Flow<ByteArray> = incoming.asSharedFlow()

    override suspend fun close() {
        gatt?.close()
        gatt = null
        commandChar = null
    }

    private fun enableNotifications(g: BluetoothGatt, statusChar: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(statusChar, true)
        val cccd = statusChar.getDescriptor(CCCD_UUID) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            run {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(cccd)
            }
        }
    }

    private fun completeConnect(result: Result<Unit>) {
        pendingConnect?.takeIf { !it.isCompleted }?.complete(result)
    }

    private companion object {
        val SERVICE_UUID: UUID = UUID.fromString(ServoBleUuids.SERVICE)
        val COMMAND_UUID: UUID = UUID.fromString(ServoBleUuids.COMMAND)
        val STATUS_UUID: UUID = UUID.fromString(ServoBleUuids.STATUS)

        // Client Characteristic Configuration Descriptor (standard 0x2902).
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
