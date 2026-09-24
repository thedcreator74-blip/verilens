package com.example.core.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object EnableOverlay : Screen("enable_overlay")
    data object Permissions : Screen("permissions")
    data object Home : Screen("home")
    data object Verify : Screen("verify")
    data object AskVeriLens : Screen("ask_verilens?historyId={historyId}") {
        fun createRoute(historyId: Long? = null): String =
            if (historyId != null) "ask_verilens?historyId=$historyId" else "ask_verilens"
    }
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object Report : Screen("report/{historyId}") {
        fun createRoute(historyId: Long): String = "report/$historyId"
    }
}

sealed class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: String,
    val unselectedIcon: String
) {
    data object Home : BottomNavItem(
        screen = Screen.Home,
        label = "Home",
        selectedIcon = "home",
        unselectedIcon = "home_outlined"
    )
    data object Verify : BottomNavItem(
        screen = Screen.Verify,
        label = "Verify",
        selectedIcon = "search",
        unselectedIcon = "search_outlined"
    )
    data object AskVeriLens : BottomNavItem(
        screen = Screen.AskVeriLens,
        label = "Ask VeriLens",
        selectedIcon = "chat",
        unselectedIcon = "chat_outlined"
    )
    data object History : BottomNavItem(
        screen = Screen.History,
        label = "History",
        selectedIcon = "history",
        unselectedIcon = "history_outlined"
    )
    data object Settings : BottomNavItem(
        screen = Screen.Settings,
        label = "Settings",
        selectedIcon = "settings",
        unselectedIcon = "settings_outlined"
    )
}
