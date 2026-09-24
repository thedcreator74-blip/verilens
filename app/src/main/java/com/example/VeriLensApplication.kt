package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
            val channelId = OVERLAY_NOTIFICATION_CHANNEL_ID
            val name = getString(R.string.overlay_channel_name)
            val descriptionText = getString(R.string.overlay_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val OVERLAY_NOTIFICATION_CHANNEL_ID = "verilens_overlay_channel"
        const val OVERLAY_NOTIFICATION_ID = 1001
    }
}
