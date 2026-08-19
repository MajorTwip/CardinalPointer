package com.cardinalpointer.ui

/**
 * Presentation-level view of the transport link, driven by whatever is
 * attempting a real hardware connection on the current platform (e.g. an
 * Android BLE auto-connect flow). Deliberately lives in `:ui` rather than
 * `:core` — it's connection-flow display state, not part of the transport
 * contract.
 */
sealed class LinkStatus {
    data object Idle : LinkStatus()
    data object RequestingPermission : LinkStatus()
    data object Scanning : LinkStatus()
    data object Connecting : LinkStatus()
    data object Connected : LinkStatus()
    data class PermissionDenied(val detail: String? = null) : LinkStatus()
    data class DeviceNotFound(val detail: String? = null) : LinkStatus()
    data class ConnectFailed(val detail: String? = null) : LinkStatus()
    data object Disconnected : LinkStatus()

    /** Permanent default for platforms (e.g. desktop) that never attempt a real link. */
    data object SimulatedOnly : LinkStatus()
}

val LinkStatus.isLive: Boolean get() = this is LinkStatus.Connected

val LinkStatus.isRetryable: Boolean get() = this is LinkStatus.PermissionDenied ||
    this is LinkStatus.DeviceNotFound ||
    this is LinkStatus.ConnectFailed ||
    this is LinkStatus.Disconnected

/** Short enough to fit the header badge on one line — the dot color already
 * signals live/in-progress/retryable, so the text doesn't need to repeat it. */
val LinkStatus.label: String get() = when (this) {
    LinkStatus.Idle -> "INITIALIZING"
    LinkStatus.RequestingPermission -> "PERMISSION…"
    LinkStatus.Scanning -> "SCANNING…"
    LinkStatus.Connecting -> "CONNECTING…"
    LinkStatus.Connected -> "CONNECTED"
    is LinkStatus.PermissionDenied -> "PERMISSION DENIED — RETRY"
    is LinkStatus.DeviceNotFound -> "${detail ?: "NOT FOUND"} — RETRY"
    is LinkStatus.ConnectFailed -> "CONNECT FAILED — RETRY"
    LinkStatus.Disconnected -> "DISCONNECTED — RETRY"
    LinkStatus.SimulatedOnly -> "SIMULATED"
}
