package ir.daroyar.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import ir.daroyar.app.R

private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F8B7D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F2E6),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF05201C),
    tertiary = Color(0xFF8A5100),
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFF5FAF8),
    surface = Color(0xFFF5FAF8),
    surfaceContainer = Color(0xFFE9F0ED),
    surfaceContainerHigh = Color(0xFFE3EAE7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF84D6C9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFA0F2E5),
    secondary = Color(0xFFB1CCC6),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFFFFB960),
    tertiaryContainer = Color(0xFF693C00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    surfaceContainer = Color(0xFF1B211F),
    surfaceContainerHigh = Color(0xFF252B29),
)

private fun TextStyle.vazir() = copy(fontFamily = Vazirmatn)

private val AppTypography = Typography().run {
    Typography(
        displayLarge = displayLarge.vazir(),
        displayMedium = displayMedium.vazir(),
        displaySmall = displaySmall.vazir(),
        headlineLarge = headlineLarge.vazir(),
        headlineMedium = headlineMedium.vazir(),
        headlineSmall = headlineSmall.vazir(),
        titleLarge = titleLarge.vazir(),
        titleMedium = titleMedium.vazir(),
        titleSmall = titleSmall.vazir(),
        bodyLarge = bodyLarge.vazir(),
        bodyMedium = bodyMedium.vazir(),
        bodySmall = bodySmall.vazir(),
        labelLarge = labelLarge.vazir(),
        labelMedium = labelMedium.vazir(),
        labelSmall = labelSmall.vazir(),
    )
}

/** App theme; the layout is always right-to-left, whatever the phone's language. */
@Composable
fun DaroYarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
    }
}
