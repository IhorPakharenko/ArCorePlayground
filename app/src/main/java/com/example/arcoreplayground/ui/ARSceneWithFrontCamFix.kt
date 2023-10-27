package com.example.arcoreplayground.ui

//import io.github.sceneview.ar.ARScene
//import io.github.sceneview.ar.ArSceneView
//import io.github.sceneview.ar.arcore.ArFrame
//import io.github.sceneview.ar.arcore.ArSession
//import io.github.sceneview.node.Node

/**
 * A copy of ARScene.
 * Initiates the session for the selfie camera in a deprecated way.
 * This way the session does not crash with a TextureNotSetException on startup.
 */
//@Composable
//fun ARSceneWithFrontCamFix(
//    modifier: Modifier = Modifier,
//    nodes: List<Node> = listOf(),
//    cameraFacingDirection: CameraConfig.FacingDirection = CameraConfig.FacingDirection.BACK,
//    planeRenderer: Boolean = true,
//    onCreate: ((ArSceneView) -> Unit)? = null,
//    onSessionCreate: (ArSceneView.(session: ArSession) -> Unit)? = null,
//    onTrackingFailureChanged: (ArSceneView.(trackingFailureReason: TrackingFailureReason?) -> Unit)? = null,
//    onFrame: (ArSceneView.(arFrame: ArFrame) -> Unit)? = null,
//    onTap: (ArSceneView.(hitResult: HitResult) -> Unit)? = null
//) {
//    if (LocalInspectionMode.current) {
//        ARScene()
//    } else {
//        var sceneViewNodes = remember { listOf<Node>() }
//
//        AndroidView(
//            modifier = modifier,
//            factory = { context ->
//                ArSceneView(
//                    context,
//                    sessionFeatures = if (cameraFacingDirection == CameraConfig.FacingDirection.FRONT) {
//                        setOf(Session.Feature.FRONT_CAMERA)
//                    } else {
//                        emptySet()
//                    }
//                ).apply {
//                    this.onArSessionCreated = { onSessionCreate?.invoke(this, it) }
//                    this.onArFrame = { onFrame?.invoke(this, it) }
//                    this.onArTrackingFailureChanged = { onTrackingFailureChanged?.invoke(this, it) }
//                    this.onTapAr = { hitResult, _ -> onTap?.invoke(this, hitResult) }
//                    onCreate?.invoke(this)
//                }
//            },
//            update = { sceneView ->
//                sceneViewNodes.filter { it !in nodes }.forEach {
//                    sceneView.removeChild(it)
//                }
//                nodes.filter { it !in sceneViewNodes }.forEach {
//                    sceneView.addChild(it)
//                }
//                sceneViewNodes = nodes
//
//                sceneView.planeRenderer.isEnabled = planeRenderer
//            }
//        )
//    }
//}