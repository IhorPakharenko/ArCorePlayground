package com.example.arcoreplayground.ui.models

import android.content.res.Configuration
import android.media.CamcorderProfile
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.arcoreplayground.VideoRecorder
import com.example.arcoreplayground.util.saveBitmap
import com.example.arcoreplayground.util.screenshot
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

class AppArBackFacingFragment : ArFragment() {

    private var model: Renderable? = null
    private val recorder: VideoRecorder = VideoRecorder()

    private var setModelJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments = (arguments ?: bundleOf()).apply {
            putBoolean(BaseArFragment.ARGUMENT_FULLSCREEN, false)
        }
        super.onViewCreated(view, savedInstanceState)

        recorder.setSceneView(arSceneView)
        recorder.setVideoQuality(
            CamcorderProfile.QUALITY_HIGH,
            Configuration.ORIENTATION_PORTRAIT
        )

        setOnSessionConfigurationListener { session, config ->
            // Modify the AR session configuration here
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
        }
        setOnViewCreatedListener { arSceneView ->
            arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
        }
        setOnTapArPlaneListener(::onTapPlane)
    }

    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (model == null) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the Anchor.
        arSceneView.scene.addChild(AnchorNode(hitResult.createAnchor()).apply {
            // Create the transformable model and add it to the anchor.
            addChild(TransformableNode(transformationSystem).apply {
                renderable = model
                renderableInstance.setCulling(false)
                renderableInstance.animate(true).start()
            })
        })
    }

    fun setModel(modelPath: String) {
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            Log.e(AppArBackFacingFragment::class.simpleName, "Setting model failed", exception)
        }
        setModelJob?.cancel()
        setModelJob = lifecycleScope.launch(exceptionHandler) {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                model = ModelRenderable.builder()
                    .setSource(context, Uri.parse(modelPath))
                    .setIsFilamentGltf(true)
                    .build()
                    .await()
            }
        }
    }

    suspend fun takeScreenshot(): Result<Uri> =
        runCatching {
            assert(lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                "Can't take a screenshot if before the AR view is created"
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