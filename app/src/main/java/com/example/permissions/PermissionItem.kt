package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val isRequired: Boolean = false
)

class PermissionManager(private val context: Context) {

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

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

    fun getPermissionList(): List<PermissionItem> {
        return listOf(
            PermissionItem(
                id = "camera",
                title = "Camera Access",
                subtitle = "Scan printed newspapers, leaflets, and physical notices directly",
                icon = Icons.Default.CameraAlt,
                isGranted = hasCameraPermission(),
                isRequired = true
            ),
            PermissionItem(
                id = "overlay",
                title = "Display Over Other Apps",
                subtitle = "Floating quick-verify button for instant screen checks in WhatsApp & browsers",
                icon = Icons.Default.Layers,
                isGranted = hasOverlayPermission(),
                isRequired = false
            ),
            PermissionItem(
                id = "notifications",
                title = "Notification Alerts",
                subtitle = "Receive foreground service status and background verification alerts",
                icon = Icons.Default.Notifications,
                isGranted = hasNotificationPermission(),
                isRequired = false
            )
        )
    }
}
