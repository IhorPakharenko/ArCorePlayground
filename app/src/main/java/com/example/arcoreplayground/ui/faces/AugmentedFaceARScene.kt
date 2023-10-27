package com.example.arcoreplayground.ui.faces

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.example.arcoreplayground.databinding.FragmentAugmentedFaceArSceneBinding
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFrontFacingFragment
import com.google.ar.sceneform.ux.AugmentedFaceNode
import kotlinx.coroutines.future.await

@Composable
fun AugmentedFaceARScene(
    modifier: Modifier = Modifier,
    faceModelPath: String,
    faceTexturePath: String,
) {
    val scope = rememberCoroutineScope()

    var faceTexture by remember { mutableStateOf<Texture?>(null) }
    var faceModel by remember { mutableStateOf<ModelRenderable?>(null) }

    var faceNodes = remember { mutableStateMapOf<AugmentedFace, AugmentedFaceNode>() }

    val context = LocalContext.current

    LaunchedEffect(faceModelPath) {
        try {
            faceModel = ModelRenderable.builder()
                .setSource(context, Uri.parse(faceModelPath))
                .setIsFilamentGltf(true)
                .build()
                .await()
        } catch (e: Exception) {

        }
    }

    LaunchedEffect(faceTexturePath) {
        try {
            faceTexture = Texture.builder()
                .setSource(context, Uri.parse(faceTexturePath))
                .setUsage(Texture.Usage.COLOR_MAP)
                .build()
                .await()
        } catch (e: Exception) {

        }
    }

    AndroidViewBinding(
        factory = { inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean ->
            val binding =
                FragmentAugmentedFaceArSceneBinding.inflate(inflater, parent, attachToParent)
            binding.arFragment.setOnViewCreatedListener { arSceneView ->
                // This is important to make sure that the camera stream renders first so that
                // the face mesh occlusion works correctly.
                arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST)
            }
            // Check for face detections
            binding.arFragment.setOnAugmentedFaceUpdateListener { augmentedFace: AugmentedFace ->
                if (faceTexture == null || faceModel == null) return@setOnAugmentedFaceUpdateListener

                val existingFaceNode = faceNodes[augmentedFace]

                when (augmentedFace.trackingState) {
                    TrackingState.TRACKING -> {
                        if (existingFaceNode != null) return@setOnAugmentedFaceUpdateListener

                        val faceNode = AugmentedFaceNode(augmentedFace)

                        val modelInstance = faceNode.setFaceRegionsRenderable(faceModel)
                        modelInstance.isShadowCaster = false
                        modelInstance.isShadowReceiver = true

                        faceNode.faceMeshTexture = faceTexture

                        binding.arSceneView.scene.addChild(faceNode)

                        faceNodes[augmentedFace] = faceNode
                    }

                    TrackingState.STOPPED -> {
                        existingFaceNode?.let {
                            binding.arSceneView.scene.removeChild(it)
                        }
                        faceNodes -= augmentedFace
                    }

                    TrackingState.PAUSED -> Unit
                }
            }
            return@AndroidViewBinding binding
        },
        modifier = modifier,
        update = {

        }
    )
}

private val FragmentAugmentedFaceArSceneBinding.arFragment get() = fragmentContainerView.getFragment<ArFrontFacingFragment>()
private val FragmentAugmentedFaceArSceneBinding.arSceneView get() = arFragment.arSceneView
