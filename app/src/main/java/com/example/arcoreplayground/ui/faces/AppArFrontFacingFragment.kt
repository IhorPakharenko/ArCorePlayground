package com.example.arcoreplayground.ui.faces

import android.content.res.Configuration
import android.media.CamcorderProfile
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.arcoreplayground.VideoRecorder
import com.example.arcoreplayground.util.saveBitmap
import com.example.arcoreplayground.util.screenshot
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFrontFacingFragment
import com.google.ar.sceneform.ux.AugmentedFaceNode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

class AppArFrontFacingFragment : ArFrontFacingFragment() {

    private var faceTexture: Texture? = null
    private var faceModel: ModelRenderable? = null
    private val faceNodes = mutableMapOf<AugmentedFace, AugmentedFaceNode>()
    private val recorder: VideoRecorder = VideoRecorder()

    private var setMaskJob: Job? = null

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
                    setMask(faceNode)
                    arSceneView.scene.addChild(faceNode)
                    faceNodes[augmentedFace] = faceNode
                }

                TrackingState.STOPPED -> {
                    existingFaceNode?.let {
                        arSceneView.scene.removeChild(it)
                    }
                    faceNodes -= augmentedFace
                }

                TrackingState.PAUSED -> {}
            }
        }
    }

    fun setMask(modelPath: String, texturePath: String? = null) {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Log.e(ArFrontFacingFragment::class.simpleName, "Setting mask failed", exception)
        }
        setMaskJob?.cancel()
        setMaskJob = lifecycleScope.launch(exceptionHandler) {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                faceModel = async {
                    ModelRenderable.builder()
                        .setSource(context, Uri.parse(modelPath))
                        .setIsFilamentGltf(true)
                        .build()
                        .await()
                }.await()

                faceTexture = if (texturePath != null) {
                    async {
                        Texture.builder()
                            .setSource(context, Uri.parse(texturePath))
                            .setUsage(Texture.Usage.COLOR_MAP)
                            .build()
                            .await()
                    }.await()
                } else {
                    null
                }

                faceNodes.forEach { (_, faceNode) ->
                    setMask(faceNode)
                }
            }
        }
    }

    private fun setMask(faceNode: AugmentedFaceNode) {
        faceNode.setFaceRegionsRenderable(faceModel).apply {
            isShadowCaster = false
            isShadowReceiver = true
        }
        faceNode.faceMeshTexture = faceTexture
    }

    suspend fun takeScreenshot(): Result<Uri> =
        runCatching {
            assert(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                "Can't take a screenshot before the AR view is created"
            }
            val bitmap = arSceneView.screenshot()
            return@runCatching saveBitmap(requireContext(), bitmap)
        }

    fun startRecord() {
        if (!recorder.isRecording) {
            recorder.onToggleRecord()
        }
    }

    fun stopRecord() {
        if (recorder.isRecording) {
            recorder.onToggleRecord()
        }
    }
}