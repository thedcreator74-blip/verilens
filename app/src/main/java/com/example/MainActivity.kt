package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.core.navigation.NavGraph
import com.example.overlay.VeriLensOverlayService
import com.example.ui.theme.VeriLensTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var openOverlaySheetRequested by mutableStateOf(false)
    private var pendingScreenshotPath by mutableStateOf<String?>(null)
    private var pendingOverlayAction by mutableStateOf<String?>(null)
    private var hasOverlayPermission by mutableStateOf(false)

    companion object {
        const val EXTRA_VERIFY_SCREENSHOT_PATH = "extra_verify_screenshot_path"
        const val EXTRA_ACTION = "extra_action"
        const val ACTION_UPLOAD_SCREENSHOT = "action_upload_screenshot"
        const val ACTION_PASTE_TEXT = "action_paste_text"
        const val ACTION_PASTE_LINK = "action_paste_link"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as VeriLensApplication
        hasOverlayPermission = Settings.canDrawOverlays(this)
        handleIncomingIntent(intent)

        setContent {
            val preferences by app.container.settingsRepository.userPreferences
                .collectAsState(initial = null)

            val isDark = when (preferences?.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            VeriLensTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    container = app.container,
                    initialOpenOverlaySheet = openOverlaySheetRequested,
                    verifyScreenshotPath = pendingScreenshotPath,
                    onScreenshotPathConsumed = { pendingScreenshotPath = null },
                    overlayAction = pendingOverlayAction,
                    onOverlayActionConsumed = { pendingOverlayAction = null },
                    hasOverlayPermission = hasOverlayPermission
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val permitted = Settings.canDrawOverlays(this)
            hasOverlayPermission = permitted
            if (permitted) {
                val app = application as VeriLensApplication
                app.container.overlayManager.startOverlay()
                lifecycleScope.launch {
                    app.container.settingsRepository.setOverlayEnabled(true)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onResume overlay check", e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        val screenshotPath = intent.getStringExtra(EXTRA_VERIFY_SCREENSHOT_PATH)
        if (!screenshotPath.isNullOrBlank()) {
            pendingScreenshotPath = screenshotPath
        }

        val action = intent.getStringExtra(EXTRA_ACTION)
        if (!action.isNullOrBlank()) {
            pendingOverlayAction = action
        }

        val extraOpenSheet = intent.getBooleanExtra(VeriLensOverlayService.EXTRA_OPEN_SHEET, false)
        val deepLinkHost = intent.data?.host
        if (extraOpenSheet || deepLinkHost == "verify") {
            openOverlaySheetRequested = true
        }
    }
}
