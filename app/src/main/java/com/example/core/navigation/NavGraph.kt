package com.example.core.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.MainActivity
import com.example.core.di.AppContainer
import com.example.feature.chat.ui.AskVeriLensScreen
import com.example.feature.chat.viewmodel.ChatViewModel
import com.example.feature.verification.capture.ScreenCaptureManager
import com.example.ui.components.OverlayBottomSheet
import com.example.ui.components.VerificationProgressDialog
import com.example.ui.components.VeriLensBottomBar
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.history.HistoryScreen
import com.example.ui.screens.history.HistoryViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.home.PasteLinkVerifyDialog
import com.example.ui.screens.home.PasteTextVerifyDialog
import com.example.ui.screens.onboarding.EnableFloatingAssistantScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.onboarding.OnboardingViewModel
import com.example.ui.screens.permissions.PermissionCenterScreen
import com.example.ui.screens.permissions.PermissionViewModel
import com.example.ui.screens.report.ReportScreen
import com.example.ui.screens.report.ReportViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File
import android.graphics.BitmapFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    container: AppContainer,
    initialOpenOverlaySheet: Boolean = false,
    verifyScreenshotPath: String? = null,
    onScreenshotPathConsumed: () -> Unit = {},
    overlayAction: String? = null,
    onOverlayActionConsumed: () -> Unit = {},
    hasOverlayPermission: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Overlay Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(initialOpenOverlaySheet) }
    var showNavTextDialog by remember { mutableStateOf(false) }
    var showNavLinkDialog by remember { mutableStateOf(false) }

    // Live verification pipeline states
    var isVerifying by remember { mutableStateOf(false) }
    val verificationProgress by container.verificationRepository.verificationProgress.collectAsStateWithLifecycle()
    val screenCaptureManager = remember { ScreenCaptureManager(context) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isVerifying = true
                try {
                    val id = container.verificationRepository.verifyScreenshotUri(uri)
                    navController.navigate(Screen.Report.createRoute(id))
                } catch (e: Exception) {
                    Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isVerifying = false
                }
            }
        }
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            coroutineScope.launch {
                isVerifying = true
                try {
                    val displayMetrics = context.resources.displayMetrics
                    val bitmap = screenCaptureManager.captureScreen(
                        resultCode = result.resultCode,
                        resultData = result.data!!,
                        width = displayMetrics.widthPixels,
                        height = displayMetrics.heightPixels,
                        densityDpi = displayMetrics.densityDpi
                    )
                    if (bitmap != null) {
                        val id = container.verificationRepository.verifyScreenshotBitmap(bitmap)
                        navController.navigate(Screen.Report.createRoute(id))
                    } else {
                        Toast.makeText(context, "Could not capture display frame. Please try photo upload.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Screen capture verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isVerifying = false
                }
            }
        } else {
            Toast.makeText(context, "Screen capture permission was cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    if (showNavTextDialog) {
        PasteTextVerifyDialog(
            onDismiss = { showNavTextDialog = false },
            onVerify = { text ->
                showNavTextDialog = false
                coroutineScope.launch {
                    isVerifying = true
                    try {
                        val id = container.verificationRepository.verifyText(text)
                        navController.navigate(Screen.Report.createRoute(id))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isVerifying = false
                    }
                }
            }
        )
    }

    if (showNavLinkDialog) {
        PasteLinkVerifyDialog(
            onDismiss = { showNavLinkDialog = false },
            onVerify = { url ->
                showNavLinkDialog = false
                coroutineScope.launch {
                    isVerifying = true
                    try {
                        val id = container.verificationRepository.verifyLink(url)
                        navController.navigate(Screen.Report.createRoute(id))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isVerifying = false
                    }
                }
            }
        )
    }

    LaunchedEffect(initialOpenOverlaySheet) {
        if (initialOpenOverlaySheet) {
            isSheetOpen = true
        }
    }

    // Handle screenshot captured directly from system floating overlay
    LaunchedEffect(verifyScreenshotPath) {
        val path = verifyScreenshotPath ?: return@LaunchedEffect
        onScreenshotPathConsumed()
        val file = File(path)
        if (file.exists()) {
            coroutineScope.launch {
                isVerifying = true
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val id = container.verificationRepository.verifyScreenshotBitmap(bitmap)
                        navController.navigate(Screen.Report.createRoute(id))
                    } else {
                        Toast.makeText(context, "Could not decode captured screenshot", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isVerifying = false
                    try { file.delete() } catch (_: Exception) {}
                }
            }
        }
    }

    // Handle quick action triggered from system floating overlay bottom sheet
    LaunchedEffect(overlayAction) {
        val action = overlayAction ?: return@LaunchedEffect
        onOverlayActionConsumed()
        when (action) {
            MainActivity.ACTION_UPLOAD_SCREENSHOT -> {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            MainActivity.ACTION_PASTE_TEXT -> {
                showNavTextDialog = true
            }
            MainActivity.ACTION_PASTE_LINK -> {
                showNavLinkDialog = true
            }
        }
    }

    // Automatically navigate back to Home once overlay permission is granted
    LaunchedEffect(hasOverlayPermission) {
        if (hasOverlayPermission) {
            container.overlayManager.startOverlay()
            coroutineScope.launch {
                container.settingsRepository.setOverlayEnabled(true)
            }
            if (currentRoute == Screen.EnableOverlay.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.EnableOverlay.route) { inclusive = true }
                }
            }
        }
    }

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Verify.route,
        Screen.AskVeriLens.route,
        Screen.History.route,
        Screen.Settings.route
    )
    val shouldShowBottomBar = currentRoute in bottomBarRoutes || currentRoute?.startsWith("ask_verilens") == true

    val isOverlayEnabled = hasOverlayPermission || container.overlayManager.isOverlayPermitted()
    val initialDestination = if (isOverlayEnabled) Screen.Home.route else Screen.EnableOverlay.route

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                VeriLensBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToItem = { item ->
                        when (item) {
                            BottomNavItem.Verify -> {
                                if (container.overlayManager.isOverlayPermitted()) {
                                    container.overlayManager.startOverlay()
                                    isSheetOpen = true
                                } else {
                                    navController.navigate(Screen.EnableOverlay.route)
                                }
                            }
                            else -> {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = initialDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (shouldShowBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
            ) {
                // Dedicated System-Wide Floating Assistant Onboarding Screen
                composable(Screen.EnableOverlay.route) {
                    EnableFloatingAssistantScreen(
                        onPermissionGranted = {
                            container.overlayManager.startOverlay()
                            coroutineScope.launch {
                                container.settingsRepository.setOverlayEnabled(true)
                            }
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
                // Onboarding Screen
                composable(Screen.Onboarding.route) {
                    val onboardingViewModel = remember {
                        OnboardingViewModel(
                            settingsRepository = container.settingsRepository
                        )
                    }
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        onNavigateToPermissions = {
                            navController.navigate(Screen.Permissions.route)
                        }
                    )
                }

                // Home Screen
                composable(Screen.Home.route) {
                    val homeViewModel = remember {
                        HomeViewModel(
                            verificationRepository = container.verificationRepository,
                            settingsRepository = container.settingsRepository,
                            overlayManager = container.overlayManager
                        )
                    }
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToReport = { historyId ->
                            navController.navigate(Screen.Report.createRoute(historyId))
                        },
                        onNavigateToHistory = {
                            navController.navigate(Screen.History.route)
                        },
                        onOpenOverlaySheet = {
                            isSheetOpen = true
                        },
                        onNavigateToAskVeriLens = {
                            navController.navigate(Screen.AskVeriLens.route)
                        }
                    )
                }

                // Ask VeriLens Screen (Chatbot / Evidence Assistant)
                composable(
                    route = Screen.AskVeriLens.route,
                    arguments = listOf(
                        navArgument("historyId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        }
                    )
                ) { backStackEntry ->
                    val historyId = backStackEntry.arguments?.getLong("historyId") ?: -1L
                    val chatViewModel = remember(historyId) {
                        ChatViewModel(
                            chatRepository = container.chatRepository,
                            initialReportId = if (historyId > 0L) historyId else null
                        )
                    }
                    AskVeriLensScreen(
                        viewModel = chatViewModel,
                        onNavigateBack = {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.Home.route)
                            }
                        }
                    )
                }

                // History Screen
                composable(Screen.History.route) {
                    val historyViewModel = remember {
                        HistoryViewModel(
                            verificationRepository = container.verificationRepository
                        )
                    }
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onNavigateToReport = { historyId ->
                            navController.navigate(Screen.Report.createRoute(historyId))
                        }
                    )
                }

                // Settings Screen
                composable(Screen.Settings.route) {
                    val settingsViewModel = remember {
                        SettingsViewModel(
                            settingsRepository = container.settingsRepository,
                            verificationRepository = container.verificationRepository,
                            overlayManager = container.overlayManager
                        )
                    }
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToPermissions = {
                            navController.navigate(Screen.Permissions.route)
                        },
                        onNavigateToAbout = {
                            navController.navigate(Screen.About.route)
                        }
                    )
                }

                // Permission Center Screen
                composable(Screen.Permissions.route) {
                    val permissionViewModel = remember {
                        PermissionViewModel(
                            permissionManager = container.permissionManager
                        )
                    }
                    PermissionCenterScreen(
                        viewModel = permissionViewModel,
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Permissions.route) { inclusive = true }
                            }
                        },
                        showBackButton = true,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Report Details Screen
                composable(
                    route = Screen.Report.route,
                    arguments = listOf(navArgument("historyId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val historyId = backStackEntry.arguments?.getLong("historyId") ?: 1L
                    val reportViewModel = remember(historyId) {
                        ReportViewModel(
                            verificationRepository = container.verificationRepository,
                            historyId = historyId
                        )
                    }
                    ReportScreen(
                        viewModel = reportViewModel,
                        onBackClick = { navController.popBackStack() },
                        onAskVeriLens = { id ->
                            navController.navigate(Screen.AskVeriLens.createRoute(id))
                        }
                    )
                }

                // About Screen
                composable(Screen.About.route) {
                    AboutScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Live Verification Progress Dialog
            if (isVerifying) {
                VerificationProgressDialog(
                    progress = verificationProgress
                )
            }

            // Global Overlay Bottom Sheet
            if (isSheetOpen) {
                OverlayBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = {
                        coroutineScope.launch {
                            sheetState.hide()
                            isSheetOpen = false
                        }
                    },
                    onActionSelected = { actionName ->
                        coroutineScope.launch {
                            sheetState.hide()
                            isSheetOpen = false
                            when (actionName) {
                                "Capture Screenshot" -> {
                                    try {
                                        mediaProjectionLauncher.launch(screenCaptureManager.createScreenCaptureIntent())
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Launching image selector...", Toast.LENGTH_SHORT).show()
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                }
                                "Upload Screenshot" -> {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                "Paste Text" -> {
                                    showNavTextDialog = true
                                }
                                "Paste Link" -> {
                                    showNavLinkDialog = true
                                }
                                else -> {
                                    showNavTextDialog = true
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
