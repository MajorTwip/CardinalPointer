package com.cardinalpointer.core.usecase

import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.domain.CameraState
import com.cardinalpointer.core.transport.Command
import com.cardinalpointer.core.transport.Transport

class SetCameraOrientation(private val transport: Transport) {

    suspend fun setSwivel(direction: CameraDirection, degrees: Float): Result<Unit> {
        val clamped = degrees.coerceIn(CameraState.SWIVEL_MIN, CameraState.SWIVEL_MAX)
        return transport.send(Command.SetCameraSwivel(direction, clamped))
    }

    suspend fun setDepression(direction: CameraDirection, degrees: Float): Result<Unit> {
        val clamped = degrees.coerceIn(CameraState.DEPRESSION_MIN, CameraState.DEPRESSION_MAX)
        return transport.send(Command.SetCameraDepression(direction, clamped))
    }
}
