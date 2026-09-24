package com.example.overlay

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

class OverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "OverlayManager"
    }

    fun isOverlayPermitted(): Boolean {
        val permitted = Settings.canDrawOverlays(context)
        Log.d(TAG, "isOverlayPermitted: $permitted")
        return permitted
    }

    fun isServiceRunning(): Boolean {
        if (VeriLensOverlayService.isOverlayActive) return true
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (VeriLensOverlayService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun startOverlay(): Boolean {
        if (!isOverlayPermitted()) {
            Log.w(TAG, "Cannot start overlay service: Permission granted: false")
            return false
        }
        Log.d(TAG, "Permission granted: true. Starting service...")
        val intent = Intent(context, VeriLensOverlayService::class.java).apply {
            action = VeriLensOverlayService.ACTION_START
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Overlay service started successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay service: ${e.message}", e)
            false
        }
    }

    fun stopOverlay() {
        Log.d(TAG, "Stopping overlay service")
        try {
            val intent = Intent(context, VeriLensOverlayService::class.java).apply {
                action = VeriLensOverlayService.ACTION_STOP
            }
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping overlay service", e)
        }
    }
}
