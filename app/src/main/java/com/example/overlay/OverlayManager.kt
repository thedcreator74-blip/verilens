package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.VeriLensApplication

class OverlayManager(private val context: Context) {

    fun isOverlayPermitted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun startOverlay() {
        if (!isOverlayPermitted()) return
        val intent = Intent(context, VeriLensOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopOverlay() {
        val intent = Intent(context, VeriLensOverlayService::class.java)
        context.stopService(intent)
    }
}

class VeriLensOverlayService : Service() {

    companion object {
        const val EXTRA_OPEN_SHEET = "extra_open_sheet"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
    }

    private fun startAsForeground() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_SHEET, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(
            this,
            VeriLensApplication.OVERLAY_NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("VeriLens Assistant Active")
            .setContentText("Tap to quick-verify suspicious messages or screen content.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(VeriLensApplication.OVERLAY_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
