import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppColorTheme(val label: String) {
    Noir("Noir"),
    Sage("Sage"),
    Citrus("Citrus")
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

private val SageLight = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFF2F855A),
    background = Color(0xFFF6FAF8),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF7F1),
    outlineVariant = Color(0xFFD9E8DF)
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

private val DarkColorScheme = darkColorScheme()

@Composable
fun AppTheme(
    theme: AppColorTheme = AppColorTheme.Noir,
    darkTheme: Boolean = false,
    content: @Composable() () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        when (theme) {
            AppColorTheme.Noir -> NoirLight
            AppColorTheme.Sage -> SageLight
            AppColorTheme.Citrus -> CitrusLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
