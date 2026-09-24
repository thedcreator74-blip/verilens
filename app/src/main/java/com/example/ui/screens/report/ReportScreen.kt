package com.example.ui.screens.report

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CredibilityMeter
import com.example.ui.components.EvidenceItemCard
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.CyanLight
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.Navy800

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report by viewModel.reportState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Verification Report", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("report_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Verdict Banner
                    VerdictSummaryCard(report = currentReport)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Credibility Meter
                    CredibilityMeter(score = currentReport.credibilityScore)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Investigated Claim Section
                    SectionHeader(title = "Original Claim / Input")
                    Spacer(modifier = Modifier.height(8.dp))
                    ClaimContentCard(text = currentReport.originalClaim, inputType = currentReport.inputType)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fact-Check Summary / Verified Info
                    SectionHeader(title = "Verified Findings")
                    Spacer(modifier = Modifier.height(8.dp))
                    VerifiedFindingsCard(findings = currentReport.verifiedInformation, reasoning = currentReport.reasoning)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Citations / Evidence
                    SectionHeader(title = "Authoritative Evidence (${currentReport.sources.size})")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(currentReport.sources) { item ->
                    EvidenceItemCard(
                        item = item,
                        onClick = { /* Open web citation if needed */ }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    if (currentReport.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SectionHeader(title = "Recommendations Before Sharing")
                        Spacer(modifier = Modifier.height(8.dp))
                        RecommendationsCard(recommendations = currentReport.recommendations)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Disclaimer
                    Text(
                        text = currentReport.disclaimer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun VerdictSummaryCard(report: ReportDetail) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().testTag("verdict_summary_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerdictBadge(verdict = report.verdict)
                Text(
                    text = "Source: ${report.inputType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ClaimContentCard(text: String, inputType: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun VerifiedFindingsCard(findings: String, reasoning: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Navy800,
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Key Verified Facts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CyanLight
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = findings,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            if (reasoning.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Reasoning & Context",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CyanLight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun RecommendationsCard(recommendations: List<String>) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            recommendations.forEach { rec ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rec,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
