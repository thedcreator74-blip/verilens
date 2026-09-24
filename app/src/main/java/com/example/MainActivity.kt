package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.core.navigation.NavGraph
import com.example.ui.theme.VeriLensTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as VeriLensApplication
        val container = app.container

        setContent {
            val userPrefs by container.settingsRepository.userPreferences.collectAsState(
                initial = null
            )

            val isDark = when (userPrefs?.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            VeriLensTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    container = container
                )
            }
        }
    }
}
