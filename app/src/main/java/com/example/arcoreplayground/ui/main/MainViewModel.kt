package com.example.arcoreplayground.ui.main

import com.example.arcoreplayground.MviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class MainViewModel @Inject constructor(
) : MviViewModel<MainUiState, MainPartialState, MainEvent, MainIntent>(
    MainUiState(
        placeables = listOf(
            Placeable(
                displayName = "Panda",
                path = "https://storage.googleapis.com/ar-answers-in-search-models/static/GiantPanda/model.glb"
            ),
            Placeable(displayName = "Snail", path = "models/high_detailed_snail.glb"),
            Placeable(
                displayName = "Monkey",
                path = "models/detailed_monkey.glb",
                scaleToUnits = (0.75f)
            )
        ),
        activeMask = FaceMask(
            displayName = "Abc",
            modelPath = "models/fox.glb",
            texturePath = "textures/freckles.png"
        )
    )
) {
    override fun mapIntents(intent: MainIntent): Flow<MainPartialState> = when (intent) {
        is MainIntent.AddFaceMask -> flowOf(MainPartialState.FaceMaskAdded(intent.mask))
        is MainIntent.AddPlaceable -> flowOf(MainPartialState.PlaceableAdded(intent.placeable))
        is MainIntent.RemoveFaceMask -> flowOf(MainPartialState.FaceMaskRemoved(intent.mask))
        is MainIntent.RemovePlaceable -> flowOf(MainPartialState.PlaceableRemoved(intent.placeable))
        MainIntent.StartRecording -> flowOf(MainPartialState.RecordingStarted)
        MainIntent.StopRecording -> flowOf(MainPartialState.RecordingFinished)
        is MainIntent.SwitchCamera -> flowOf(MainPartialState.CameraSwitched(intent.camera))
        is MainIntent.SetActiveMask -> flowOf(MainPartialState.ActiveMaskSet(intent.mask))
        is MainIntent.SetActivePlaceable -> flowOf(MainPartialState.ActivePlaceableSet(intent.placeable))
    }

    override fun reduceUiState(
        previousState: MainUiState,
        partialState: MainPartialState
    ): MainUiState = when (partialState) {
        is MainPartialState.CameraSwitched -> previousState.copy(camera = partialState.camera)
        is MainPartialState.FaceMaskAdded -> previousState.copy(
            masks = previousState.masks + partialState.mask
        )

        is MainPartialState.FaceMaskRemoved -> previousState.copy(
            masks = previousState.masks - partialState.mask
        )

        is MainPartialState.PlaceableAdded -> previousState.copy(
            placeables = previousState.placeables + partialState.placeable
        )

        is MainPartialState.PlaceableRemoved -> previousState.copy(
            placeables = previousState.placeables - partialState.placeable
        )

        MainPartialState.RecordingFinished -> previousState.copy(isRecording = false)
        MainPartialState.RecordingStarted -> previousState.copy(isRecording = true)
        is MainPartialState.ActiveMaskSet -> previousState.copy(activeMask = partialState.mask)
        is MainPartialState.ActivePlaceableSet -> previousState.copy(activePlaceable = partialState.placeable)
    }
}