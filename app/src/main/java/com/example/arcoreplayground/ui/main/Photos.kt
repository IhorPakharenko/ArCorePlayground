package com.example.arcoreplayground.ui.main

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
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

//@Throws(IOException::class)
//private fun createImageFile(): File? {
//    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//    val imageFileName = "JPEG_" + timeStamp + "_"
//    val storageDir: File =
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) Environment.getExternalStoragePublicDirectory(
//            Environment.DIRECTORY_PICTURES
//        ) else getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//    val image = File.createTempFile(
//        imageFileName,  /* prefix */
//        ".jpg",  /* suffix */
//        storageDir /* directory */
//    )
//    currentPhotoPath = image.absolutePath
//    return image
//}

@Throws(IOException::class)
fun saveBitmap(
    context: Context, bitmap: Bitmap
): Uri {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_$timeStamp.jpg"

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
    }

    val resolver = context.contentResolver
    var uri: Uri? = null

    try {
        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create new MediaStore record.")

        resolver.openOutputStream(uri)?.use {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it))
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

//private fun saveToGallery(context: Context) {
//    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
//        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
//        intent.data = Uri.parse("file://" + photoFile.getAbsolutePath())
//        context.sendBroadcast(intent)
//    } else {
//        val contentValues = ContentValues()
//        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, photoFile.getName())
//        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/*")
//        val contentUri: Uri
//        contentUri = if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//        } else {
//            MediaStore.Images.Media.INTERNAL_CONTENT_URI
//        }
//        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
//        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)
//        val uri: Uri = context.contentResolver.insert(contentUri, contentValues)!!
//        val os: OutputStream = context.contentResolver.openOutputStream(uri)!!
//        try {
//            val bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath())
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
//            contentValues.clear()
//            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
//            context.contentResolver.update(uri, contentValues, null, null)
//        } catch (e: java.lang.Exception) {
//            context.contentResolver.delete(uri, null, null)
//            e.printStackTrace()
//        } finally {
//            try {
//                os?.close()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
////        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
////        mediaScanIntent.data = contentUri
////        context.sendBroadcast(mediaScanIntent)
//    }
//}