package com.example.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.core.di.AppContainer
import com.example.feature.chat.ui.AskVeriLensScreen
import com.example.feature.chat.viewmodel.ChatViewModel
import com.example.ui.components.VeriLensBottomBar
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.camera.CameraVerificationScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.onboarding.EnableFloatingAssistantScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.onboarding.OnboardingViewModel
import com.example.ui.screens.permissions.PermissionCenterScreen
import com.example.ui.screens.permissions.PermissionViewModel
import com.example.ui.screens.report.ReportScreen
import com.example.ui.screens.report.ReportViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.AskVeriLens.route,
        Screen.History.route,
        Screen.Settings.route
    )
    val shouldShowBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                VeriLensBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToItem = { item ->
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                val homeViewModel = rememberHomeViewModel(container)
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToCamera = {
                        navController.navigate(Screen.Camera.route)
                    },
                    onNavigateToReport = { historyId ->
                        navController.navigate(Screen.Report.createRoute(historyId))
                    },
                    onNavigateToAskAi = {
                        navController.navigate(Screen.AskVeriLens.route)
                    }
                )
            }

            composable(Screen.Camera.route) {
                CameraVerificationScreen(
                    container = container,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onVerificationStarted = { historyId ->
                        navController.navigate(Screen.Report.createRoute(historyId)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(
                route = Screen.AskVeriLens.route,
                arguments = listOf(
                    navArgument("historyId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                val chatViewModel = rememberChatViewModel(container)
                AskVeriLensScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.History.route) {
                val historyViewModel = rememberHistoryViewModel(container)
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToReport = { historyId ->
                        navController.navigate(Screen.Report.createRoute(historyId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel = rememberSettingsViewModel(container)
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPermissions = {
                        navController.navigate(Screen.Permissions.route)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route)
                    }
                )
            }

            composable(
                route = Screen.Report.route,
                arguments = listOf(
                    navArgument("historyId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val historyId = backStackEntry.arguments?.getLong("historyId") ?: 0L
                val reportViewModel = rememberReportViewModel(historyId, container)
                ReportScreen(
                    viewModel = reportViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Permissions.route) {
                val permissionViewModel = rememberPermissionViewModel(container)
                PermissionCenterScreen(
                    viewModel = permissionViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                val onboardingViewModel = rememberOnboardingViewModel(container)
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.EnableOverlay.route) {
                EnableFloatingAssistantScreen(
                    onEnable = {
                        container.overlayManager.startOverlay()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.EnableOverlay.route) { inclusive = true }
                        }
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.EnableOverlay.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberHomeViewModel(container: AppContainer): HomeViewModel {
    return androidx.compose.runtime.remember {
        HomeViewModel(
            verificationRepository = container.verificationRepository,
            settingsRepository = container.settingsRepository,
            overlayManager = container.overlayManager
        )
    }
}

@Composable
private fun rememberChatViewModel(container: AppContainer): ChatViewModel {
    return androidx.compose.runtime.remember {
        ChatViewModel(chatRepository = container.chatRepository)
    }
}

@Composable
private fun rememberHistoryViewModel(container: AppContainer): HistoryViewModel {
    return androidx.compose.runtime.remember {
        HistoryViewModel(verificationRepository = container.verificationRepository)
    }
}

@Composable
private fun rememberSettingsViewModel(container: AppContainer): SettingsViewModel {
    return androidx.compose.runtime.remember {
        SettingsViewModel(
            settingsRepository = container.settingsRepository,
            overlayManager = container.overlayManager
        )
    }
}

@Composable
private fun rememberPermissionViewModel(container: AppContainer): PermissionViewModel {
    return androidx.compose.runtime.remember {
        PermissionViewModel(permissionManager = container.permissionManager)
    }
}

@Composable
private fun rememberOnboardingViewModel(container: AppContainer): OnboardingViewModel {
    return androidx.compose.runtime.remember {
        OnboardingViewModel(settingsRepository = container.settingsRepository)
    }
}

@Composable
private fun rememberReportViewModel(historyId: Long, container: AppContainer): ReportViewModel {
    return androidx.compose.runtime.remember(historyId) {
        ReportViewModel(
            historyId = historyId,
            verificationRepository = container.verificationRepository
        )
    }
}
