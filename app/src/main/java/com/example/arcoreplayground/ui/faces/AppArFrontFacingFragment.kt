package com.example.arcoreplayground.ui.faces

import android.content.res.Configuration
import android.media.CamcorderProfile
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.arcoreplayground.VideoRecorder
import com.example.arcoreplayground.ui.main.saveBitmap
import com.example.arcoreplayground.ui.main.screenshot
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFrontFacingFragment
import com.google.ar.sceneform.ux.AugmentedFaceNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

class AppArFrontFacingFragment : ArFrontFacingFragment() {

    private var faceTexture: Texture? = null
    private var faceModel: ModelRenderable? = null
    private val faceNodes = mutableMapOf<AugmentedFace, AugmentedFaceNode>()
    private val recorder: VideoRecorder = VideoRecorder()

    private var modelPath = MutableStateFlow<String?>(null)
    private var texturePath = MutableStateFlow<String?>(null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.
        arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST)

        recorder.setSceneView(arSceneView)
        recorder.setVideoQuality(
            CamcorderProfile.QUALITY_HIGH,
            Configuration.ORIENTATION_PORTRAIT
        )

        setOnAugmentedFaceUpdateListener { augmentedFace: AugmentedFace ->
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

                    arSceneView.scene.addChild(faceNode)

                    faceNodes[augmentedFace] = faceNode
                }

                TrackingState.STOPPED -> {
                    existingFaceNode?.let {
                        arSceneView.scene.removeChild(it)
                    }
                    faceNodes -= augmentedFace
                }

                TrackingState.PAUSED -> Unit
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    texturePath.mapLatest { path ->
                        Texture.builder()
                            .setSource(context, Uri.parse(path ?: return@mapLatest null))
                            .setUsage(Texture.Usage.COLOR_MAP)
                            .build()
                            .await()
                    }.catch {

                    }.collect {
                        faceTexture = it
                    }
                }
                launch {
                    modelPath.mapLatest { path ->
                        ModelRenderable.builder()
                            .setSource(context, Uri.parse(path))
                            .setIsFilamentGltf(true)
                            .build()
                            .await()
                    }.catch {

                    }.collect {
                        faceModel = it
                    }
                }
            }
        }
    }

    fun setMask(modelPath: String, texturePath: String? = null) {
        this.modelPath.value = modelPath
        this.texturePath.value = texturePath
    }

    suspend fun takeScreenshot(): Result<Uri> =
        runCatching {
            assert(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                "Can't take a screenshot if before the AR view is created"
            }
            val bitmap = arSceneView.screenshot()
            return@runCatching saveBitmap(requireContext(), bitmap)
        }

    fun toggleRecord() {
        recorder.onToggleRecord()
    }

    object Keys {
        const val MODEL_PATH = "MODEL_PATH"
        const val TEXTURE_PATH = "TEXTURE_PATH"
    }
}