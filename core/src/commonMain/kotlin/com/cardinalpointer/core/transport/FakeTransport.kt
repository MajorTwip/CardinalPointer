package com.cardinalpointer.core.transport

import com.cardinalpointer.core.domain.MastState
import com.cardinalpointer.core.domain.MotionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * In-memory stand-in for a real transport so the app is usable before
 * UsbSerialTransport exists. Echoes camera commands straight back as status
 * updates and simulates mast motion over a couple of seconds.
 */
class FakeTransport(private val scope: CoroutineScope) : Transport {

    private val updates = MutableSharedFlow<StatusUpdate>(extraBufferCapacity = 64)

    private var currentHeight = MastState.DEFAULT_MIN_HEIGHT
    private var targetHeight = MastState.DEFAULT_MIN_HEIGHT
    private var motionJob: Job? = null

    override suspend fun send(command: Command): Result<Unit> {
        when (command) {
            is Command.SetCameraSwivel ->
                updates.emit(StatusUpdate.CameraSwivel(command.direction, command.degrees))

            is Command.SetCameraDepression ->
                updates.emit(StatusUpdate.CameraDepression(command.direction, command.degrees))

            is Command.SetMastHeight -> {
                targetHeight = command.height
                updates.emit(StatusUpdate.MastHeight(currentHeight, targetHeight))
            }

            Command.Erect -> startMotion(MotionState.Erecting, targetHeight)

            Command.Fold -> {
                targetHeight = MastState.DEFAULT_MIN_HEIGHT
                startMotion(MotionState.Folding, MastState.DEFAULT_MIN_HEIGHT)
            }
        }
        return Result.success(Unit)
    }

    override fun subscribeStatusUpdates(): Flow<StatusUpdate> = updates

    private fun startMotion(state: MotionState, destination: Float) {
        motionJob?.cancel()
        motionJob = scope.launch {
            updates.emit(StatusUpdate.MastMotion(state))
            val steps = 24
            val start = currentHeight
            for (step in 1..steps) {
                if (!isActive) return@launch
                delay(60)
                currentHeight = start + (destination - start) * step / steps
                updates.emit(StatusUpdate.MastHeight(currentHeight, targetHeight))
            }
            updates.emit(StatusUpdate.MastMotion(MotionState.Idle))
        }
    }
}
