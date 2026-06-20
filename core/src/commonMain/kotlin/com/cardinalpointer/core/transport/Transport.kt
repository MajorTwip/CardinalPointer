package com.cardinalpointer.core.transport

import kotlinx.coroutines.flow.Flow

/** Transport contract — platform adapters implement this interface.
 *
 * Current adapters planned:
 * - `UsbSerialTransport` (Android + Windows)
 * - `AndroidIntentTransport` (future, same contract)
 */
interface Transport {
    /** Send a command to the mast controller. */
    suspend fun send(command: Command): Result<Unit>

    /** Continuous stream of status updates pushed by the controller. */
    fun subscribeStatusUpdates(): Flow<StatusUpdate>
}
