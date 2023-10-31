@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)

package com.example.arcoreplayground.ui.main

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.arcoreplayground.R
import com.example.arcoreplayground.databinding.ActivityMainBinding
import com.example.arcoreplayground.ui.faces.AppArFrontFacingFragment
import com.example.arcoreplayground.ui.models.AppArBackFacingFragment
import com.example.arcoreplayground.ui.theme.ArCorePlaygroundTheme
import com.example.arcoreplayground.util.getFilePathFromURI
import com.example.arcoreplayground.util.mapLikeLifeData
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.ar.core.CameraConfig.FacingDirection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File


@AndroidEntryPoint
//TODO save recordings the same way screenshots are saved -- screenshots show up in the gallery,
// but recordings don't
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val camera = viewModel.uiState.mapLikeLifeData { it.camera }
        val activeMask = viewModel.uiState.mapLikeLifeData { it.activeMask }
        val activePlaceable = viewModel.uiState.mapLikeLifeData { it.activePlaceable }
        val isRecording = viewModel.uiState.mapLikeLifeData { it.isRecording }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    camera.collect { camera ->
                        val fragment = when (camera) {
                            FacingDirection.FRONT -> AppArFrontFacingFragment().apply {
                                launch {
                                    activeMask.collect {
                                        setMask(
                                            modelPath = it.modelPath,
                                            texturePath = it.texturePath
                                        )
                                    }
                                }
                                launch {
                                    isRecording.collect { isRecording ->
                                        if (isRecording) {
                                            startRecord()
                                        } else {
                                            stopRecord()
                                        }
                                    }
                                }
                                launch {
                                    viewModel.event.collect {
                                        if (it == MainEvent.TakeScreenshot) {
                                            takeScreenshot()
                                        }
                                    }
                                }
                            }

                            FacingDirection.BACK -> AppArBackFacingFragment().apply {
                                launch {
                                    activePlaceable.collect {
                                        setModel(it.path)
                                    }
                                }
                                launch {
                                    isRecording.collect { isRecording ->
                                        if (isRecording) {
                                            startRecord()
                                        } else {
                                            stopRecord()
                                        }
                                    }
                                }
                                launch {
                                    viewModel.event.collect {
                                        if (it == MainEvent.TakeScreenshot) {
                                            takeScreenshot()
                                        }
                                    }
                                }
                            }
                        }
                        supportFragmentManager.commit {
                            replace(R.id.arContainer, fragment)
                        }
                    }
                }
            }
        }

        binding.composeContainer.setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ArCorePlaygroundTheme {
                MainScreen(
                    uiState = uiState,
                    onIntent = viewModel::acceptIntent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onIntent: (MainIntent) -> Unit,
) {
    BottomSheetScaffold(
        containerColor = Color.Transparent,
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
                    FrontCameraSheet(uiState = uiState, onIntent = onIntent)
                }

                FacingDirection.BACK -> {
                    BackCameraSheet(uiState = uiState, onIntent = onIntent)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        CameraButtons(uiState = uiState, onIntent = onIntent, modifier = Modifier.padding(it))
    }
}

@Composable
private fun FrontCameraSheet(
    uiState: MainUiState,
    onIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier
) = LazyColumn(modifier) {
    items(uiState.masks, key = { it.modelPath }) { mask ->
        //TODO find out why derivedStateOf always produces false here
        val isSelected = uiState.activeMask == mask
        val textColor by animateColorAsState(
            if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        Text(
            text = mask.displayName,
            modifier = Modifier
                .clickable {
                    onIntent(MainIntent.SetActiveMask(mask))
                }
                .padding(12.dp)
                .fillMaxWidth(),
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
    item {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
    }
}

@Composable
private fun BackCameraSheet(
    uiState: MainUiState,
    onIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier
) = LazyColumn(modifier) {
    items(uiState.placeables, key = { it.path }) { placeable ->
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
        val context = LocalContext.current
        val pickerResultListener =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                val path = getFilePathFromURI(
                    context,
                    uri ?: return@rememberLauncherForActivityResult
                ) ?: return@rememberLauncherForActivityResult
                val placeable = Placeable(
                    displayName = path.split(File.separator).lastOrNull()
                        ?: "Unknown",
                    path = uri.toString()
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

@Composable
private fun CameraButtons(
    uiState: MainUiState,
    onIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier
) = Box(modifier) {
    Column(Modifier.align(Alignment.TopStart)) {
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
            IconButton(
                onClick = {
                    onIntent(MainIntent.TakeScreenshot)
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
                //TODO
//                        placeholderNode?.anchor()
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