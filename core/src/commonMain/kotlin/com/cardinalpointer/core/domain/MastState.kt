package com.cardinalpointer.core.domain

data class MastState(
    val currentHeight: Float,
    val targetHeight: Float,
    val motionState: MotionState,
    val minHeight: Float,
    val maxHeight: Float
) {
    companion object {
        const val DEFAULT_MIN_HEIGHT = 2f
        const val DEFAULT_MAX_HEIGHT = 12f
    }
}
