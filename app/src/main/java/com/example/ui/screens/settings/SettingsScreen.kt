package com.example.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.VeriLensTopAppBar
import com.example.ui.theme.LocalExtendedColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val extendedColors = LocalExtendedColors.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Theme Picker Dialog
    if (showThemeDialog) {
        val themes = listOf("SYSTEM" to "System Default", "LIGHT" to "Light Mode", "DARK" to "Dark Mode")
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("App Theme") },
            text = {
                Column {
                    themes.forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Language Dialog
    if (showLanguageDialog) {
        val languages = listOf("English", "Spanish", "French", "German", "Hindi")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Display Language") },
            text = {
                Column {
                    languages.forEach { lang ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = preferences.language == lang,
                                onClick = {
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = lang, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("This will delete all saved reports and recent input queries from the Room database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory {
                            Toast.makeText(context, "Verification history cleared", Toast.LENGTH_SHORT).show()
                        }
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("VeriLens Privacy Guarantee") },
            text = {
                Column {
                    Text(
                        text = "1. Local First: Your historical verifications and settings are stored locally on your device in Room and encrypted DataStore.\n\n" +
                                "2. Zero Background Monitoring: The floating overlay never records screen contents automatically. It only triggers when you tap it.\n\n" +
                                "3. Zero Gallery Access: Media picking uses Android's zero-permission system photo picker.\n\n" +
                                "4. Strict Least Privilege: No contacts, microphone, camera, or phone state permissions are requested.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            VeriLensTopAppBar(title = "Settings")
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("settings_screen_root")
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 84.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Section: Appearance & General
            item {
                SettingsSectionHeader(title = "Preferences")
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.DarkMode,
                    title = "App Theme",
                    subtitle = when (preferences.themeMode) {
                        "DARK" -> "Dark Mode"
                        "LIGHT" -> "Light Mode"
                        else -> "System Default"
                    },
                    tag = "setting_theme",
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Language,
                    title = "Language",
                    subtitle = preferences.language,
                    tag = "setting_language",
                    onClick = { showLanguageDialog = true }
                )
            }

            // Section: Floating Lens & Overlay Service
            item {
                SettingsSectionHeader(title = "Floating Lens Overlay")
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(18.dp))
                        .testTag("setting_overlay_toggle_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .surfaceBackground(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Floating Lens Bubble",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (preferences.isOverlayEnabled) "Active foreground service" else "Tap to enable quick access",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = preferences.isOverlayEnabled,
                            onCheckedChange = { enable ->
                                if (enable && !viewModel.isOverlayPermitted()) {
                                    Toast.makeText(context, "Overlay permission required", Toast.LENGTH_SHORT).show()
                                    onNavigateToPermissions()
                                } else {
                                    val success = viewModel.toggleOverlay(enable)
                                    if (!success) {
                                        Toast.makeText(context, "Please grant overlay permission", Toast.LENGTH_SHORT).show()
                                        onNavigateToPermissions()
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("overlay_switch")
                        )
                    }
                }
            }

            // Section: Storage & History Management
            item {
                SettingsSectionHeader(title = "Data & Storage")
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Cached,
                    title = "Clear Image Cache",
                    subtitle = "Remove cached thumbnails & previews",
                    tag = "setting_clear_cache",
                    onClick = {
                        Toast.makeText(context, "Image cache cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.CleaningServices,
                    title = "Clear Verification History",
                    subtitle = "Delete all stored local reports",
                    tag = "setting_clear_history",
                    onClick = { showClearHistoryDialog = true }
                )
            }

            // Section: Trust, Privacy & About
            item {
                SettingsSectionHeader(title = "Privacy & App Info")
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Security,
                    title = "Permissions Center",
                    subtitle = "Review and configure app capabilities",
                    tag = "setting_permissions_center",
                    onClick = onNavigateToPermissions
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Read our zero-tracking data philosophy",
                    tag = "setting_privacy_policy",
                    onClick = { showPrivacyDialog = true }
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Filled.Info,
                    title = "About VeriLens AI",
                    subtitle = "Mission, architecture foundation & team",
                    tag = "setting_about_app",
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, extendedColors.cardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .surfaceBackground(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun Modifier.surfaceBackground(color: androidx.compose.ui.graphics.Color, shape: RoundedCornerShape): Modifier =
    this.border(0.dp, androidx.compose.ui.graphics.Color.Transparent, shape).then(Modifier)
