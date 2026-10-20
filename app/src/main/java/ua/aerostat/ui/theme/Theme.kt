package ua.aerostat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Палітра з ТЗ. */
object AeroColors {
    val Graphite = Color(0xFF101820)
    val Card = Color(0xFF1D2932)
    val CardHigh = Color(0xFF26343E)
    val Mint = Color(0xFF83D5AE)
    val Text = Color(0xFFF3F7F5)
    val Secondary = Color(0xFFA5B5BC)
    val Outline = Color(0xFF3A4A55)
    val Warning = Color(0xFFF2C46D)
    val Error = Color(0xFFFF9B8F)

    val LightBackground = Color(0xFFF4F7F6)
    val LightCard = Color(0xFFFFFFFF)
    val LightCardHigh = Color(0xFFE7EEEB)
    val LightPrimary = Color(0xFF2B7D59)
    val LightText = Color(0xFF101820)
    val LightSecondary = Color(0xFF4F5F66)
    val LightOutline = Color(0xFFC5D1CD)
    val LightWarning = Color(0xFF8A5A00)
    val LightError = Color(0xFFB3261E)
}

enum class ThemeMode(val title: String) {
    SYSTEM("Як у системі"),
    DARK("Темна"),
    LIGHT("Світла"),
}

private val DarkScheme = darkColorScheme(
    primary = AeroColors.Mint,
    onPrimary = Color(0xFF0A1F16),
    primaryContainer = Color(0xFF214A39),
    onPrimaryContainer = AeroColors.Mint,
    secondary = AeroColors.Secondary,
    onSecondary = AeroColors.Graphite,
    secondaryContainer = AeroColors.CardHigh,
    onSecondaryContainer = AeroColors.Text,
    tertiary = AeroColors.Warning,
    background = AeroColors.Graphite,
    onBackground = AeroColors.Text,
    surface = AeroColors.Graphite,
    onSurface = AeroColors.Text,
    surfaceVariant = AeroColors.Card,
    onSurfaceVariant = AeroColors.Secondary,
    surfaceContainerLowest = AeroColors.Graphite,
    surfaceContainerLow = Color(0xFF17222A),
    surfaceContainer = AeroColors.Card,
    surfaceContainerHigh = AeroColors.CardHigh,
    surfaceContainerHighest = Color(0xFF2E3D48),
    outline = AeroColors.Outline,
    outlineVariant = Color(0xFF2B3944),
    error = AeroColors.Error,
    onError = Color(0xFF3B0A05),
)

private val LightScheme = lightColorScheme(
    primary = AeroColors.LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEEDD),
    onPrimaryContainer = Color(0xFF0B3524),
    secondary = AeroColors.LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = AeroColors.LightCardHigh,
    onSecondaryContainer = AeroColors.LightText,
    tertiary = AeroColors.LightWarning,
    background = AeroColors.LightBackground,
    onBackground = AeroColors.LightText,
    surface = AeroColors.LightBackground,
    onSurface = AeroColors.LightText,
    surfaceVariant = AeroColors.LightCard,
    onSurfaceVariant = AeroColors.LightSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FBFA),
    surfaceContainer = AeroColors.LightCard,
    surfaceContainerHigh = AeroColors.LightCardHigh,
    surfaceContainerHighest = Color(0xFFDDE6E2),
    outline = AeroColors.LightOutline,
    outlineVariant = Color(0xFFDCE4E1),
    error = AeroColors.LightError,
    onError = Color.White,
)

private val base = Typography()
private val AeroTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp),
)

/** Великі числа з табличними цифрами. */
val NumberStyle = TextStyle(fontFeatureSettings = "tnum", fontWeight = FontWeight.Medium, fontSize = 22.sp)

private val AeroShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun AerostatTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDark(mode)) DarkScheme else LightScheme,
        typography = AeroTypography,
        shapes = AeroShapes,
        content = content,
    )
}
