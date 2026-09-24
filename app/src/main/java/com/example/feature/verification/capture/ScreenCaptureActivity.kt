package com.example.feature.verification.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScreenCaptureActivity : ComponentActivity() {

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultData = result.data!!
            val resultCode = result.resultCode
            lifecycleScope.launch {
                // Short delay to allow system dialog to fully dismiss from display
                delay(300)
                try {
                    val displayMetrics = resources.displayMetrics
                    val captureManager = ScreenCaptureManager(this@ScreenCaptureActivity)
                    val bitmap = captureManager.captureScreen(
                        resultCode = resultCode,
                        resultData = resultData,
                        width = displayMetrics.widthPixels,
                        height = displayMetrics.heightPixels,
                        densityDpi = displayMetrics.densityDpi
                    )

                    if (bitmap != null) {
                        val file = captureManager.saveBitmapToCache(bitmap)
                        // Launch VeriLens MainActivity with the captured screenshot file path
                        val mainIntent = Intent(this@ScreenCaptureActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra(MainActivity.EXTRA_VERIFY_SCREENSHOT_PATH, file.absolutePath)
                        }
                        startActivity(mainIntent)
                    } else {
                        Toast.makeText(
                            this@ScreenCaptureActivity,
                            "Could not capture screen. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ScreenCaptureActivity,
                        "Screen capture failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    finish()
                }
            }
        } else {
            // User cancelled the prompt. Do NOT open VeriLens.
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            captureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        } catch (e: Exception) {
            Toast.makeText(this, "Screen capture unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
