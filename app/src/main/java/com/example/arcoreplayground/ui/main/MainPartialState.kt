package com.example.arcoreplayground.ui.main

import com.google.ar.core.CameraConfig

sealed class MainPartialState {
    data class CameraSwitched(val camera: CameraConfig.FacingDirection) : MainPartialState()
    object RecordingStarted : MainPartialState()
    object RecordingFinished : MainPartialState()
    data class FaceMaskAdded(val mask: FaceMask) : MainPartialState()
    data class FaceMaskRemoved(val mask: FaceMask) : MainPartialState()
    data class ActiveMaskSet(val mask: FaceMask) : MainPartialState()
    data class PlaceableAdded(val placeable: Placeable) : MainPartialState()
    data class PlaceableRemoved(val placeable: Placeable) : MainPartialState()
    data class ActivePlaceableSet(val placeable: Placeable?) : MainPartialState()
}