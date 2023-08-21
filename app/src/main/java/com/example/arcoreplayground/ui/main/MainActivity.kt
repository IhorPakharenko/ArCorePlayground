@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)

package com.example.arcoreplayground.ui.main

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.arcoreplayground.ui.ARSceneWithFrontCamFix
import com.example.arcoreplayground.ui.theme.ArCorePlaygroundTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.filament.Engine
import com.google.ar.core.AugmentedFace
import com.google.ar.core.CameraConfig.FacingDirection
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.math.Position
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ArCorePlaygroundTheme {
                MainScreen(uiState = uiState, onIntent = viewModel::acceptIntent)
            }
        }
    }
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    onIntent: (MainIntent) -> Unit
) {
    BottomSheetScaffold(
        sheetContent = {
            Text(
                text = when (uiState.camera) {
                    FacingDirection.FRONT -> "Masks"
                    FacingDirection.BACK -> "Placeables"
                },
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge
            )
            when (uiState.camera) {
                FacingDirection.FRONT -> {

                }

                FacingDirection.BACK -> {
                    LazyColumn {
                        items(uiState.placeables, key = { it.path }) { placeable ->
//                            val isSelected by remember {
////                                derivedStateOf { uiState.activePlaceable == placeable }
//                                derivedStateOf { uiState.activePlaceable == placeable }
//                            }
                            //TODO find out why derivedStateOf always produces false here
                            val isSelected = uiState.activePlaceable == placeable
                            val textColor by animateColorAsState(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )

                            Text(
                                text = placeable.displayName,
                                modifier = Modifier
                                    .clickable {
                                        onIntent(MainIntent.SetActivePlaceable(placeable))
                                    }
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                color = textColor,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        item {
                            val pickerResultListener =
                                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                                    val placeable = Placeable(
                                        path = uri?.path ?: return@rememberLauncherForActivityResult
                                    )
                                    onIntent(MainIntent.AddPlaceable(placeable))
                                }
                            Row(
                                Modifier.clickable {
                                    pickerResultListener.launch("application/*")
                                }
                            ) {
                                Image(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
                                )
                                Text(
                                    text = "Import new",
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        item {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        var sceneView by remember { mutableStateOf<ArSceneView?>(null) }
        var engine by remember { mutableStateOf<Engine?>(null) }
        var nodes by remember { mutableStateOf(listOf<ArNode>()) }
        var placeholderNode by remember { mutableStateOf<ArModelNode?>(null) }

        sceneView?.let { scene ->
            SceneRecorder(
                isRecording = uiState.isRecording,
                sceneView = scene,
                getOutputFile = {
                    val storageDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    File.createTempFile(
                        "VID", /* prefix */
                        ".webm", /* suffix */
                        storageDir /* directory */
                    ).absolutePath
                },
                onRecorded = {
                    onIntent(MainIntent.StopRecording)
                }
            )
        }

        LaunchedEffect(uiState.activePlaceable) {
            if (placeholderNode?.isAnchored == false) {
                placeholderNode?.destroy()
                nodes = nodes - (placeholderNode ?: return@LaunchedEffect)
            }

            uiState.activePlaceable ?: return@LaunchedEffect

            placeholderNode = ArModelNode(engine!!).apply {
                isScaleEditable = true
                loadModelGlbAsync(
                    glbFileLocation = uiState.activePlaceable.path,
                    autoAnimate = true,
                    scaleToUnits = uiState.activePlaceable.scaleToUnits,
                    centerOrigin = Position(y = -1.0f)
                )
            }

            val previewNodes = nodes.filterNot { it.isAnchored }
            previewNodes.forEach { it.destroy() }

            nodes = nodes.filterNot { node ->
                node.isAnchored
            }

            nodes = nodes + (placeholderNode ?: return@LaunchedEffect)
        }
        Box {
            key(uiState.camera) { // Completely recreate the Ar Scene on camera change
                ARSceneWithFrontCamFix(
                    nodes = nodes,
                    cameraFacingDirection = uiState.camera,
                    onCreate = { view ->
                        sceneView = view
                        engine = view.engine
                    },
                    onSessionCreate = { session ->
                        //TODO use
                        session.getAllTrackables(AugmentedFace::class.java)
                    },
                    onFrame = {
                        it.frame
//                        it.frame.acquireCameraImage()
                    }
                )
            }

            Column(Modifier.align(Alignment.TopStart)) {
                val storagePermissions = rememberPermissionState(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    IconButton(
                        onClick = {
                            onIntent(
                                MainIntent.SwitchCamera(
                                    when (uiState.camera) {
                                        FacingDirection.FRONT -> FacingDirection.BACK
                                        FacingDirection.BACK -> FacingDirection.FRONT
                                    }
                                )
                            )
                        }
                    ) {
                        Image(
                            imageVector = Icons.Filled.FlipCameraAndroid,
                            contentDescription = null
                        )
                    }
                    Spacer(Modifier.height(40.dp))
                    val screenshotScope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            if (!storagePermissions.status.isGranted) {
                                storagePermissions.launchPermissionRequest()
                                return@IconButton
                            }
                            screenshotScope.launch {
                                val bitmap = sceneView?.screenshot()

                                bitmap.toString()
                                //TODO save the screenshot
                            }
//                            onIntent(
//                                TODO()
//                            )
                        }
                    ) {
                        Image(
                            imageVector = Icons.Filled.Screenshot,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(LocalContentColor.current)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    IconButton(
                        onClick = {
                            onIntent(
                                if (uiState.isRecording) {
                                    MainIntent.StopRecording
                                } else {
                                    MainIntent.StartRecording
                                }
                            )
                        }
                    ) {
                        Image(
                            imageVector = if (uiState.isRecording) {
                                Icons.Default.VideocamOff
                            } else {
                                Icons.Default.Videocam
                            },
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(LocalContentColor.current)
                        )
                    }
                }
            }
            if (uiState.activePlaceable != null) {
                LargeFloatingActionButton(
                    onClick = {
                        placeholderNode?.anchor()
                        onIntent(MainIntent.SetActivePlaceable(null))
                    },
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 40.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Image(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(LocalContentColor.current)
                    )
                }
            }
        }
    }
}

@Composable
private fun createImageFile(): File {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//    MediaStore.Images
//    val storageDir: File = LocalContext.current.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    //TODO IMG / VID
    return File.createTempFile(
        "JPEG_${timeStamp}_", /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    ).apply {
        // Save a file: path for use with ACTION_VIEW intents
//        currentPhotoPath = absolutePath
    }
}

@Throws(IOException::class)
fun saveBitmap(
    context: Context, bitmap: Bitmap, format: Bitmap.CompressFormat,
    mimeType: String, displayName: String
): Uri {

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
    }

    val resolver = context.contentResolver
    var uri: Uri? = null

    try {
        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create new MediaStore record.")

        resolver.openOutputStream(uri)?.use {
            if (!bitmap.compress(format, 95, it))
                throw IOException("Failed to save bitmap.")
        } ?: throw IOException("Failed to open output stream.")

        return uri

    } catch (e: IOException) {

        uri?.let { orphanUri ->
            // Don't leave an orphan entry in the MediaStore
            resolver.delete(orphanUri, null, null)
        }

        throw e
    }
}