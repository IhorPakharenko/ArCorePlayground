package com.example.arcoreplayground.ui.main

import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.sceneview.SceneView

sealed class State {
    object IDLE : State()
    data class Recording(val outputFile: String) : State()
}

@Composable
fun SceneRecorder(
    isRecording: Boolean,
    sceneView: SceneView,
    getOutputFile: () -> String,
    onRecorded: (String) -> Unit
) {
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
//    var hasStartedRecording by remember { mutableStateOf(false) }
//    var outputFilePath by remember { mutableStateOf<String?>(false) }
    var state by remember { mutableStateOf<State>(State.IDLE) }

    fun startRecording() {
        if (state is State.Recording) {
            state = State.IDLE
        }

        val outputFile = getOutputFile()

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(sceneView.context)
        } else {
            MediaRecorder()
        }.apply {
            setOutputFormat(MediaRecorder.OutputFormat.WEBM)
            setOutputFile(getOutputFile())
        }
        val safeRecorder = recorder ?: return
        sceneView.startRecording(safeRecorder)
        state = State.Recording(outputFile = outputFile)
    }

    fun stopRecording() {
        recorder?.let { sceneView.stopRecording(it) }
        recorder?.release()
        recorder = null

        (state as? State.Recording)?.let {
            onRecorded(it.outputFile)
            state = State.IDLE
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                stopRecording()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}