package com.example.arcoreplayground.ui.main

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Uses PixelCopy to take a photo / screenshot of the SurfaceView.
 * Another way to do this is using sceneView.currentFrame.frame and converting the image
 * from YUV_420_88 to an ordinary bitmap.
 * @return a bitmap that is a pixel-by-pixel copy of the SurfaceView.
 */
suspend fun SurfaceView.screenshot() = suspendCoroutine { continuation ->
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    PixelCopy.request(
        this,
        bitmap,
        { result ->
            if (result == PixelCopy.SUCCESS) {
                continuation.resume(bitmap)
            } else {
                continuation.resumeWithException(Exception("PixelCopy error: $result"))
            }
        },
        Handler(Looper.getMainLooper())
    )
}