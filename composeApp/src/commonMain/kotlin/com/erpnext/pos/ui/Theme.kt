import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class AppColorTheme(val label: String) {
    Noir("Noir"),
    Sage("Sage"),
    Citrus("Citrus"),
    Ocean("Ocean"),
    Rose("Rose")
}

enum class AppThemeMode(val label: String) {
    System("Sistema"),
    Light("Claro"),
    Dark("Oscuro")
}

private val NoirLight = lightColorScheme(
    primary = Color(0xFF111827),
    onPrimary = Color.White,
    secondary = Color(0xFF4B5563),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF3F4F6),
    outlineVariant = Color(0xFFE5E7EB)
)

private val NoirDark = darkColorScheme(
    primary = Color(0xFFE5E7EB),
    onPrimary = Color(0xFF0B0F16),
    secondary = Color(0xFF9CA3AF),
    background = Color(0xFF0B0F16),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    outlineVariant = Color(0xFF374151)
)

private val SageLight = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF2F855A),
    background = Color(0xFFF6FAF8),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF7F1),
    outlineVariant = Color(0xFFD9E8DF)
)

private val SageDark = darkColorScheme(
    primary = Color(0xFF8FE3C9),
    onPrimary = Color(0xFF083C34),
    secondary = Color(0xFF7BD3A5),
    background = Color(0xFF0B1412),
    surface = Color(0xFF0F1D19),
    surfaceVariant = Color(0xFF132824),
    outlineVariant = Color(0xFF25433C)
)

private val CitrusLight = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color.White,
    secondary = Color(0xFF9A3412),
    background = Color(0xFFFFFBF5),
    surface = Color.White,
    surfaceVariant = Color(0xFFFFF4E5),
    outlineVariant = Color(0xFFF3E5D0)
)

private val CitrusDark = darkColorScheme(
    primary = Color(0xFFFED7AA),
    onPrimary = Color(0xFF3B1E06),
    secondary = Color(0xFFF9A56A),
    background = Color(0xFF14100B),
    surface = Color(0xFF1A1410),
    surfaceVariant = Color(0xFF2A1F16),
    outlineVariant = Color(0xFF463628)
)

private val OceanLight = lightColorScheme(
    primary = Color(0xFF0369A1),
    onPrimary = Color.White,
    secondary = Color(0xFF0E7490),
    background = Color(0xFFF3F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6F0F7),
    outlineVariant = Color(0xFFD4E2EE)
)

private val OceanDark = darkColorScheme(
    primary = Color(0xFF7DD3FC),
    onPrimary = Color(0xFF0A2A3A),
    secondary = Color(0xFF67E8F9),
    background = Color(0xFF071319),
    surface = Color(0xFF0C1B22),
    surfaceVariant = Color(0xFF112733),
    outlineVariant = Color(0xFF214152)
)

private val RoseLight = lightColorScheme(
    primary = Color(0xFFBE123C),
    onPrimary = Color.White,
    secondary = Color(0xFF9F1239),
    background = Color(0xFFFFF5F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFFFE4E6),
    outlineVariant = Color(0xFFFAD0D6)
)

private val RoseDark = darkColorScheme(
    primary = Color(0xFFF9A8D4),
    onPrimary = Color(0xFF3A0B1E),
    secondary = Color(0xFFF472B6),
    background = Color(0xFF180B12),
    surface = Color(0xFF211017),
    surfaceVariant = Color(0xFF321822),
    outlineVariant = Color(0xFF4B2632)
)

private val DisplayFont = FontFamily.SansSerif
private val BodyFont = FontFamily.SansSerif

private val BaseTypography = Typography()

private val AppTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    displayMedium = BaseTypography.displayMedium.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold
    ),
    displaySmall = BaseTypography.displaySmall.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold
    ),
    headlineLarge = BaseTypography.headlineLarge.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium
    ),
    titleLarge = BaseTypography.titleLarge.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = BaseTypography.titleMedium.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = BaseTypography.titleSmall.copy(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = BaseTypography.bodyLarge.copy(
        fontFamily = BodyFont
    ),
    bodyMedium = BaseTypography.bodyMedium.copy(
        fontFamily = BodyFont
    ),
    bodySmall = BaseTypography.bodySmall.copy(
        fontFamily = BodyFont
    ),
    labelLarge = BaseTypography.labelLarge.copy(
        fontFamily = BodyFont,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = BaseTypography.labelMedium.copy(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = BaseTypography.labelSmall.copy(
        fontFamily = BodyFont
    )
)

@Composable
fun AppTheme(
    theme: AppColorTheme = AppColorTheme.Noir,
    themeMode: AppThemeMode = AppThemeMode.System,
    content: @Composable() () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Dark -> true
        AppThemeMode.Light -> false
    }

    val colorScheme = if (useDarkTheme) {
        when (theme) {
            AppColorTheme.Noir -> NoirDark
            AppColorTheme.Sage -> SageDark
            AppColorTheme.Citrus -> CitrusDark
            AppColorTheme.Ocean -> OceanDark
            AppColorTheme.Rose -> RoseDark
        }
    } else {
        when (theme) {
            AppColorTheme.Noir -> NoirLight
            AppColorTheme.Sage -> SageLight
            AppColorTheme.Citrus -> CitrusLight
            AppColorTheme.Ocean -> OceanLight
            AppColorTheme.Rose -> RoseLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
