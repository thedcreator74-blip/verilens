package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer

class VeriLensApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.overlay_channel_name)
            val descriptionText = getString(R.string.overlay_channel_desc)
            val channel = NotificationChannel(
                OVERLAY_NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val OVERLAY_NOTIFICATION_CHANNEL_ID = "verilens_overlay_channel"
        const val OVERLAY_NOTIFICATION_ID = 1001
    }
}
