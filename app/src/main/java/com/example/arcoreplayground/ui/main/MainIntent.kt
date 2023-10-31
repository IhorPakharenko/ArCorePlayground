package com.example.arcoreplayground.ui.main

import com.google.ar.core.CameraConfig

sealed class MainIntent {
    data class SwitchCamera(val camera: CameraConfig.FacingDirection) : MainIntent()
    object StartRecording : MainIntent()
    object StopRecording : MainIntent()
    object TakeScreenshot : MainIntent()
    data class AddFaceMask(val mask: FaceMask) : MainIntent()
    data class RemoveFaceMask(val mask: FaceMask) : MainIntent()
    data class SetActiveMask(val mask: FaceMask) : MainIntent()
    data class AddPlaceable(val placeable: Placeable) : MainIntent()
    data class RemovePlaceable(val placeable: Placeable) : MainIntent()
    data class SetActivePlaceable(val placeable: Placeable?) : MainIntent()
}