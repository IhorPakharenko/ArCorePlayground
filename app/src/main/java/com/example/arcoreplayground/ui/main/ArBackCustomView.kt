package com.example.arcoreplayground.ui.main

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.example.arcoreplayground.R
import com.example.arcoreplayground.databinding.FragmentModelsArSceneBinding
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ArBackCustomView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding: FragmentModelsArSceneBinding
    private val scene get() = binding.arSceneView.scene

    private var model: Renderable? = null
    private var modelView: ViewRenderable? = null

    init {
        binding =
            FragmentModelsArSceneBinding.inflate(LayoutInflater.from(context), this, true)

        binding.arFragment.apply {
            setOnSessionConfigurationListener { session, config ->
                // Modify the AR session configuration here
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            }
            setOnViewCreatedListener { arSceneView ->
//                arSceneView.setFrameRateFactor(SceneView.FrameRate.FULL)
            }
            setOnTapArPlaneListener(::onTapPlane)
        }

//viewScope.launch {
        loadModels()
//}
//        lifecycleScope.launchWhenCreated {
//        }
    }

    private fun loadModels() {
        ModelRenderable.builder()
            .setSource(context, Uri.parse("models/high_detailed_snail.glb"))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { model = it }
        ViewRenderable.builder()
            .setView(context, R.layout.view_renderable_infos)
            .build()
            .thenAccept { modelView = it }
    }

    private fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (model == null || modelView == null) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the Anchor.
        scene.addChild(AnchorNode(hitResult.createAnchor()).apply {
            // Create the transformable model and add it to the anchor.
            addChild(TransformableNode(binding.arFragment.transformationSystem).apply {
                renderable = model
                renderableInstance.setCulling(false)
                renderableInstance.animate(true).start()
                // Add the View
                addChild(Node().apply {
                    // Define the relative position
                    localPosition = Vector3(0.0f, 1f, 0.0f)
                    localScale = Vector3(0.7f, 0.7f, 0.7f)
                    renderable = modelView
                })
            })
        })
    }
}

private val FragmentModelsArSceneBinding.arFragment get() = fragmentContainerView.getFragment<ArFragment>()
private val FragmentModelsArSceneBinding.arSceneView get() = arFragment.arSceneView

val View.viewScope: CoroutineScope
    get() {
        val storedScope = getTag(42) as? CoroutineScope
        if (storedScope != null) return storedScope

        val newScope = ViewCoroutineScope()
        if (isAttachedToWindow) {
            addOnAttachStateChangeListener(newScope)
            setTag(42, newScope)
        } else newScope.cancel()

        return newScope
    }

private class ViewCoroutineScope : CoroutineScope, View.OnAttachStateChangeListener {
    override val coroutineContext = SupervisorJob() + Dispatchers.Main

    override fun onViewAttachedToWindow(view: View) = Unit

    override fun onViewDetachedFromWindow(view: View) {
        coroutineContext.cancel()
        view.setTag(42, null)
    }
}