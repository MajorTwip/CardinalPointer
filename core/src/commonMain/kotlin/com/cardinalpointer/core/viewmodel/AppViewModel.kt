package com.cardinalpointer.core.viewmodel

import com.cardinalpointer.core.domain.CameraDirection
import com.cardinalpointer.core.domain.CameraState
import com.cardinalpointer.core.domain.MastState
import com.cardinalpointer.core.domain.MotionState
import com.cardinalpointer.core.transport.StatusUpdate
import com.cardinalpointer.core.transport.Transport
import com.cardinalpointer.core.usecase.MastActionUseCase
import com.cardinalpointer.core.usecase.SetCameraOrientation
import com.cardinalpointer.core.usecase.SetMastHeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val cameras: Map<CameraDirection, CameraState> = CameraDirection.entries.associateWith { CameraState(it) },
    val mast: MastState = MastState(
        currentHeight = 0f,
        targetHeight = 0f,
        motionState = MotionState.Idle,
        minHeight = 0f,
        maxHeight = 100f
    ),
    val selectedCamera: CameraDirection = CameraDirection.North,
    val errorMessage: String? = null
)

/** Shared view-model used by both Android and Desktop shells.
 *
 * The caller supplies a [CoroutineScope] that matches the UI lifecycle
 * (e.g. `viewModelScope` on Android, `rememberCoroutineScope` on Desktop).
 */
class AppViewModel(
    private val transport: Transport,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val setCameraOrientation = SetCameraOrientation(transport)
    private val setMastHeight = SetMastHeight(transport)
    private val mastAction = MastActionUseCase(transport)

    init {
        scope.launch {
            transport.subscribeStatusUpdates().collect { applyStatusUpdate(it) }
        }
    }

    fun selectCamera(direction: CameraDirection) {
        _uiState.update { it.copy(selectedCamera = direction) }
    }

    fun setSwivel(degrees: Float) {
        scope.launch {
            val direction = _uiState.value.selectedCamera
            setCameraOrientation.setSwivel(direction, degrees)
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun setDepression(degrees: Float) {
        scope.launch {
            val direction = _uiState.value.selectedCamera
            setCameraOrientation.setDepression(direction, degrees)
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun setTargetHeight(height: Float) {
        scope.launch {
            setMastHeight.execute(height, _uiState.value.mast)
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun erect() {
        scope.launch {
            mastAction.erect()
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun fold() {
        scope.launch {
            mastAction.fold()
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun applyStatusUpdate(update: StatusUpdate) {
        _uiState.update { state ->
            when (update) {
                is StatusUpdate.CameraSwivel -> {
                    val cameras = state.cameras.toMutableMap()
                    cameras[update.direction] = cameras[update.direction]!!
                        .copy(swivelDegrees = update.degrees)
                    state.copy(cameras = cameras)
                }
                is StatusUpdate.CameraDepression -> {
                    val cameras = state.cameras.toMutableMap()
                    cameras[update.direction] = cameras[update.direction]!!
                        .copy(depressionDegrees = update.degrees)
                    state.copy(cameras = cameras)
                }
                is StatusUpdate.MastHeight -> state.copy(
                    mast = state.mast.copy(
                        currentHeight = update.current,
                        targetHeight = update.target
                    )
                )
                is StatusUpdate.MastMotion -> state.copy(
                    mast = state.mast.copy(motionState = update.state)
                )
                is StatusUpdate.Error -> state.copy(errorMessage = update.message)
            }
        }
    }
}
