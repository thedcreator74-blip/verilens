package com.example.feature.verification.capture

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ScreenCaptureManager(private val context: Context) {

    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    fun createScreenCaptureIntent(): Intent {
        return mediaProjectionManager.createScreenCaptureIntent()
    }

    /**
     * Captures a single frame from the screen using MediaProjection.
     */
    @SuppressLint("WrongConstant")
    suspend fun captureScreen(
        resultCode: Int,
        resultData: Intent,
        width: Int,
        height: Int,
        densityDpi: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        var mediaProjection: MediaProjection? = null
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null

        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData) ?: return@withContext null

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "VeriLensScreenCapture",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                Handler(Looper.getMainLooper())
            )

            // Allow display time to render frame
            delay(400)

            val image = imageReader.acquireLatestImage() ?: run {
                delay(200)
                imageReader.acquireLatestImage()
            }

            if (image == null) return@withContext null

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            // Crop to actual screen dimensions
            val cleanBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
            cleanBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
        }
    }

    /**
     * Saves captured bitmap to cache directory and returns the file.
     */
    suspend fun saveBitmapToCache(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "screenshots").apply { mkdirs() }
        val file = File(cacheDir, "verilens_capture_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file
    }
}
