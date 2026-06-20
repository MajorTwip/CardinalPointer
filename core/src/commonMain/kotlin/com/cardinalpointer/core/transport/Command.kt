package com.cardinalpointer.core.transport

import com.cardinalpointer.core.domain.CameraDirection

sealed class Command {
    data class SetCameraSwivel(val direction: CameraDirection, val degrees: Float) : Command()
    data class SetCameraDepression(val direction: CameraDirection, val degrees: Float) : Command()
    data class SetMastHeight(val height: Float) : Command()
    data object Erect : Command()
    data object Fold : Command()
}
