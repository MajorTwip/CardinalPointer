package com.cardinalpointer.core.usecase

import com.cardinalpointer.core.transport.Command
import com.cardinalpointer.core.transport.Transport

class MastActionUseCase(private val transport: Transport) {

    suspend fun erect(): Result<Unit> = transport.send(Command.Erect)

    suspend fun fold(): Result<Unit> = transport.send(Command.Fold)
}
