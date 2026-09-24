package com.example.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.database.entities.HistoryEntity
import com.example.feature.verification.model.Verdict
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToReport: (Long) -> Unit,
    onNavigateToAskAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentHistory by viewModel.recentHistory.collectAsState()
    val stats by viewModel.stats.collectAsState()

    var showTextDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.verifyScreenshotUri(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToReport.collect { historyId ->
            onNavigateToReport(historyId)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "VeriLens AI",
                            tint = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "VeriLens AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                // Hero banner
                HeroVerificationHeader()
                Spacer(modifier = Modifier.height(16.dp))

                // Primary Input Options Grid
                Text(
                    text = "Verify Anything Before Sharing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))

                VerificationMethodsGrid(
                    onCameraClick = onNavigateToCamera,
                    onScreenshotClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onTextClick = { showTextDialog = true },
                    onLinkClick = { showLinkDialog = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Banner
                StatsSummaryRow(stats = stats)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Recent Verifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (recentHistory.isEmpty()) {
                item {
                    EmptyHistoryBanner()
                }
            } else {
                items(recentHistory.take(10), key = { it.id }) { item ->
                    RecentHistoryItem(
                        history = item,
                        onClick = { onNavigateToReport(item.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showTextDialog) {
        VerifyTextDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text ->
                showTextDialog = false
                viewModel.verifyText(text)
            }
        )
    }

    if (showLinkDialog) {
        VerifyLinkDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { url ->
                showLinkDialog = false
                viewModel.verifyLink(url)
            }
        )
    }
}

@Composable
fun HeroVerificationHeader() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Navy800,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().testTag("hero_verification_banner")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "“Think Before You Share”",
                color = CyanLight,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Instant Cross-Source Fact Verification",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Analyze WhatsApp rumors, printed posters, news articles, and viral screenshots against authoritative fact-checking networks.",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun VerificationMethodsGrid(
    onCameraClick: () -> Unit,
    onScreenshotClick: () -> Unit,
    onTextClick: () -> Unit,
    onLinkClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Camera Card (Prominent primary action)
            VerificationMethodCard(
                title = "Camera Scan",
                subtitle = "Newspapers, posters & screens",
                icon = Icons.Default.CameraAlt,
                badgeText = "New",
                accentColor = CyanPrimary,
                onClick = onCameraClick,
                modifier = Modifier.weight(1f).testTag("action_verify_camera")
            )

            // Screenshot Card
            VerificationMethodCard(
                title = "Screenshot",
                subtitle = "Social chats & images",
                icon = Icons.Default.Image,
                badgeText = null,
                accentColor = Color(0xFF6366F1),
                onClick = onScreenshotClick,
                modifier = Modifier.weight(1f).testTag("action_verify_screenshot")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Text Message Card
            VerificationMethodCard(
                title = "Paste Message",
                subtitle = "Viral forwards & text",
                icon = Icons.Default.TextFields,
                badgeText = null,
                accentColor = Color(0xFF10B981),
                onClick = onTextClick,
                modifier = Modifier.weight(1f).testTag("action_verify_text")
            )

            // Link Card
            VerificationMethodCard(
                title = "Verify Link",
                subtitle = "Articles & domain trust",
                icon = Icons.Default.Link,
                badgeText = null,
                accentColor = Color(0xFFF59E0B),
                onClick = onLinkClick,
                modifier = Modifier.weight(1f).testTag("action_verify_link")
            )
        }
    }
}

@Composable
fun VerificationMethodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String?,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = badgeText,
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatsSummaryRow(stats: HomeStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatPill(
            label = "Total Verified",
            value = stats.totalVerified.toString(),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "Risks Blocked",
            value = stats.misleadingBlocked.toString(),
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = "Avg Score",
            value = if (stats.totalVerified > 0) "${stats.averageScore}%" else "-",
            color = CyanPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentHistoryItem(
    history: HistoryEntity,
    onClick: () -> Unit
) {
    val verdict = Verdict.fromString(history.verdict)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("history_item_${history.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.title.ifBlank { history.originalClaim },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Source: ${history.inputType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${history.sourcesCount} sources",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            VerdictBadge(verdict = verdict)
        }
    }
}

@Composable
fun EmptyHistoryBanner() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No verifications yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap 'Camera Scan' or 'Screenshot' above to start verifying information.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
