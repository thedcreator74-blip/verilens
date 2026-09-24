package com.example.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Verify : Screen("verify")
    data object AskVeriLens : Screen("ask_verilens?historyId={historyId}") {
        fun createRoute(historyId: Long? = null): String {
            return if (historyId != null) "ask_verilens?historyId=$historyId" else "ask_verilens"
        }
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object EnableOverlay : Screen("enable_overlay")
    data object Onboarding : Screen("onboarding")
    data object Permissions : Screen("permissions")
    data object Report : Screen("report/{historyId}") {
        fun createRoute(historyId: Long): String = "report/$historyId"
    }
    data object About : Screen("about")
    data object Camera : Screen("camera")
}

sealed class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Verify : BottomNavItem(Screen.Verify, "Verify", Icons.Filled.Search, Icons.Outlined.Search)
    data object AskVeriLens : BottomNavItem(Screen.AskVeriLens, "Ask AI", Icons.Filled.Chat, Icons.Outlined.Chat)
    data object History : BottomNavItem(Screen.History, "History", Icons.Filled.History, Icons.Outlined.History)
    data object Settings : BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}
