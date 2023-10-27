package com.example.arcoreplayground.ui.main

import android.content.Context
import android.content.res.Configuration
import android.media.CamcorderProfile
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.arcoreplayground.VideoRecorder
import com.example.arcoreplayground.databinding.FragmentAugmentedFaceArSceneBinding
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.ArFrontFacingFragment
import com.google.ar.sceneform.ux.AugmentedFaceNode
import java.util.function.Consumer
import java.util.function.Function

class ArCustomView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var faceTexture: Texture? = null
    var faceModel: ModelRenderable? = null
    val faceNodes = mutableMapOf<AugmentedFace, AugmentedFaceNode>()
    val recorder: VideoRecorder = VideoRecorder()

    init {
        //TODO lifecycle
        ModelRenderable.builder()
            .setSource(context, Uri.parse("models/fox.glb"))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept(Consumer { model: ModelRenderable ->
                faceModel = model
            })
            .exceptionally(Function<Throwable, Void?> { throwable: Throwable? ->
                null
            })
        Texture.builder()
            .setSource(context, Uri.parse("textures/freckles.png"))
            .setUsage(Texture.Usage.COLOR_MAP)
            .build()
            .thenAccept(Consumer { texture: Texture ->
                faceTexture = texture
            })
            .exceptionally(Function<Throwable, Void?> { throwable: Throwable? ->
                null
            })
        val binding =
            FragmentAugmentedFaceArSceneBinding.inflate(LayoutInflater.from(context), this, true)
        binding.arFragment.setOnViewCreatedListener { arSceneView ->
            // This is important to make sure that the camera stream renders first so that
            // the face mesh occlusion works correctly.
            arSceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST)

            recorder.setSceneView(arSceneView)
            recorder.setVideoQuality(
                CamcorderProfile.QUALITY_HIGH,
                Configuration.ORIENTATION_PORTRAIT
            )
            recorder.onToggleRecord()

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

        binding.root.postDelayed({
            recorder.onToggleRecord()
        }, 10000)
    }
}

private val FragmentAugmentedFaceArSceneBinding.arFragment get() = fragmentContainerView.getFragment<ArFrontFacingFragment>()
private val FragmentAugmentedFaceArSceneBinding.arSceneView get() = arFragment.arSceneView