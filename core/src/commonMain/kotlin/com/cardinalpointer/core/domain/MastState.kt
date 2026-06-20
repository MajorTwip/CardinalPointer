package com.cardinalpointer.core.domain

data class MastState(
    val currentHeight: Float,
    val targetHeight: Float,
    val motionState: MotionState,
    val minHeight: Float,
    val maxHeight: Float
)
