package com.cardinalpointer.core.domain

/** Per-camera orientation state.
 *
 * [swivelDegrees]     range **[-30, +30]**
 * [depressionDegrees] range **[-45, 0]** (down is negative)
 */
data class CameraState(
    val direction: CameraDirection,
    val swivelDegrees: Float = 0f,
    val depressionDegrees: Float = 0f
) {
    companion object {
        const val SWIVEL_MIN = -30f
        const val SWIVEL_MAX = 30f
        const val DEPRESSION_MIN = -45f
        const val DEPRESSION_MAX = 0f
    }
}
