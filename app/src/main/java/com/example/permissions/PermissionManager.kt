package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val benefit: String,
    val privacyNote: String,
    val isGranted: Boolean,
    val isMandatory: Boolean = false
)

class PermissionManager(private val context: Context) {

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getOverlayPermissionIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getApplicationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getPermissionItems(): List<PermissionItem> {
        return listOf(
            PermissionItem(
                id = "overlay",
                title = "Floating Lens Overlay",
                subtitle = "Draw over other apps",
                description = "Enables the quick-access floating bubble so you can verify questionable posts or messages without leaving your social or messaging apps.",
                benefit = "Instant 1-tap verification while browsing Twitter, WhatsApp, Reddit, or the web.",
                privacyNote = "The overlay bubble only renders when activated and never monitors background screen activity.",
                isGranted = hasOverlayPermission(),
                isMandatory = false
            ),
            PermissionItem(
                id = "notifications",
                title = "Verification Notifications",
                subtitle = "Background analysis alerts",
                description = "Alerts you when a background link check or deep verification completes, and keeps the floating overlay service active.",
                benefit = "Receive instant credibility alerts without having to keep the app foregrounded.",
                privacyNote = "We only send notifications initiated directly by your verification actions.",
                isGranted = hasNotificationPermission(),
                isMandatory = false
            ),
            PermissionItem(
                id = "storage",
                title = "Media & Screenshot Selection",
                subtitle = "Android Photo Picker (Zero Permission)",
                description = "VeriLens uses the modern system Photo Picker to let you safely select screenshots without granting broad access to your entire photo gallery.",
                benefit = "Full privacy. You choose exactly which single screenshot to analyze.",
                privacyNote = "Zero broad storage permissions requested. Only the specific image you pick is opened.",
                isGranted = true, // Photo Picker requires no runtime permission
                isMandatory = false
            ),
            PermissionItem(
                id = "mediaprojection",
                title = "Screen Capture Projection",
                subtitle = "On-demand live screen verification",
                description = "VeriLens takes a temporary snapshot of the active screen when you press 'Capture Screenshot' on the floating lens.",
                benefit = "Verify un-shareable or time-sensitive stories with a single tap.",
                privacyNote = "Android enforces an explicit system confirmation prompt every single time a screen capture is initiated.",
                isGranted = true,
                isMandatory = false
            )
        )
    }
}
