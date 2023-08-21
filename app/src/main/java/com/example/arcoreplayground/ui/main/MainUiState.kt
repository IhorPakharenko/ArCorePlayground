package com.example.arcoreplayground.ui.main

import androidx.compose.runtime.Immutable
import com.google.ar.core.CameraConfig
import java.io.File


@Immutable
data class MainUiState(
    val camera: CameraConfig.FacingDirection = CameraConfig.FacingDirection.BACK,
    val isRecording: Boolean = false,
    val masks: List<FaceMask> = emptyList(),
    val activeMask: FaceMask? = null,
    val placeables: List<Placeable> = emptyList(),
    val activePlaceable: Placeable? = null,
)

sealed class FaceMask {
    object Default : FaceMask()
    data class Custom(val path: String) : FaceMask()
}

data class Placeable(
    val displayName: String,
    val path: String,
    val scaleToUnits: Float? = null
) {
    constructor(
        path: String,
        scaleToUnits: Float? = null
    ) : this(
        displayName = path.split(File.separator).lastOrNull() ?: "Unknown",
        path = path,
        scaleToUnits = scaleToUnits
    )
}