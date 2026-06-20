package com.cardinalpointer.core.transport

import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.domain.MotionState

sealed class StatusUpdate {
    data class CameraSwivel(val direction: CameraDirection, val degrees: Float) : StatusUpdate()
    data class CameraDepression(val direction: CameraDirection, val degrees: Float) : StatusUpdate()
    data class MastHeight(val current: Float, val target: Float) : StatusUpdate()
    data class MastMotion(val state: MotionState) : StatusUpdate()
    data class Error(val message: String) : StatusUpdate()
}
