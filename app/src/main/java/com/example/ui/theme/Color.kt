package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ========================================================
// VeriLens AI Material 3 Color Palette
// Inspired by Google Gemini, Google Photos & Google Wallet
// ========================================================

// Light Theme Primary & Secondary (Trust, Verification, Clarity)
val PrimaryLight = Color(0xFF0F52BA)       // Sapphire Blue
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFDCE8FE)
val OnPrimaryContainerLight = Color(0xFF041E49)

val SecondaryLight = Color(0xFF0277BD)     // Cyan / Lens Blue
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD4E4F7)
val OnSecondaryContainerLight = Color(0xFF001D35)

val TertiaryLight = Color(0xFF5B3E96)      // Deep Violet Insight
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFEDE4FF)
val OnTertiaryContainerLight = Color(0xFF1E0359)

val BackgroundLight = Color(0xFFF8FAFC)    // Clean Slate 50
val OnBackgroundLight = Color(0xFF0F172A)  // Slate 900
val SurfaceLight = Color(0xFFFFFFFF)       // Crisp Pure White
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFCBD5E1)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// Dark Theme Primary & Secondary (OLED Midnight Modern)
val PrimaryDark = Color(0xFF9EC5FF)
val OnPrimaryDark = Color(0xFF003062)
val PrimaryContainerDark = Color(0xFF00478B)
val OnPrimaryContainerDark = Color(0xFFDCE8FE)

val SecondaryDark = Color(0xFF8BCEF7)
val OnSecondaryDark = Color(0xFF00344F)
val SecondaryContainerDark = Color(0xFF004C70)
val OnSecondaryContainerDark = Color(0xFFD4E4F7)

val TertiaryDark = Color(0xFFD5BFFF)
val OnTertiaryDark = Color(0xFF381B72)
val TertiaryContainerDark = Color(0xFF4B3182)
val OnTertiaryContainerDark = Color(0xFFEDE4FF)

val BackgroundDark = Color(0xFF0B0F19)     // Obsidian Slate
val OnBackgroundDark = Color(0xFFF1F5F9)
val SurfaceDark = Color(0xFF131C2E)        // Deep Midnight Card
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = Color(0xFF1E293B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF334155)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Semantic Verification Colors (Success / Warning / Info)
val SuccessGreenLight = Color(0xFF059669)
val OnSuccessGreenLight = Color(0xFFFFFFFF)
val SuccessGreenContainerLight = Color(0xFFD1FAE5)

val WarningAmberLight = Color(0xFFD97706)
val OnWarningAmberLight = Color(0xFFFFFFFF)
val WarningAmberContainerLight = Color(0xFFFEF3C7)

val InfoSkyLight = Color(0xFF0284C7)
val OnInfoSkyLight = Color(0xFFFFFFFF)
val InfoSkyContainerLight = Color(0xFFE0F2FE)

val SuccessGreenDark = Color(0xFF34D399)
val OnSuccessGreenDark = Color(0xFF064E3B)
val SuccessGreenContainerDark = Color(0xFF065F46)

val WarningAmberDark = Color(0xFFFBBF24)
val OnWarningAmberDark = Color(0xFF78350F)
val WarningAmberContainerDark = Color(0xFF92400E)

val InfoSkyDark = Color(0xFF38BDF8)
val OnInfoSkyDark = Color(0xFF082F49)
val InfoSkyContainerDark = Color(0xFF075985)

@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val cardBorder: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = SuccessGreenLight,
        onSuccess = OnSuccessGreenLight,
        successContainer = SuccessGreenContainerLight,
        onSuccessContainer = OnSuccessGreenDark,
        warning = WarningAmberLight,
        onWarning = OnWarningAmberLight,
        warningContainer = WarningAmberContainerLight,
        onWarningContainer = OnWarningAmberDark,
        info = InfoSkyLight,
        onInfo = OnInfoSkyLight,
        infoContainer = InfoSkyContainerLight,
        onInfoContainer = OnInfoSkyDark,
        cardBorder = OutlineLight.copy(alpha = 0.5f)
    )
}
