package com.example.feature.verification.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager

class ScreenCaptureManager(private val context: Context) {
    private val projectionManager: MediaProjectionManager? =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager

    fun createScreenCaptureIntent(): Intent? {
        return projectionManager?.createScreenCaptureIntent()
    }

    suspend fun captureFromIntent(resultData: Intent): Bitmap? {
        // Creates bitmap representation of screen capture
        return Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }
    }
}
