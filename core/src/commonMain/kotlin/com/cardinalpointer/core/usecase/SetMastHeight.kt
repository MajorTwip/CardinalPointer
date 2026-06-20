package com.cardinalpointer.core.usecase

import com.cardinalpointer.core.domain.MastState
import com.cardinalpointer.core.transport.Command
import com.cardinalpointer.core.transport.Transport

class SetMastHeight(private val transport: Transport) {

    suspend fun execute(height: Float, limits: MastState): Result<Unit> {
        val clamped = height.coerceIn(limits.minHeight, limits.maxHeight)
        return transport.send(Command.SetMastHeight(clamped))
    }
}
