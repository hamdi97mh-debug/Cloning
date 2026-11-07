package app.lembar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.lembar.ThemeMode

// Lembar's own palette: a deep green-teal accent on warm paper tones.
private val Light = lightColorScheme(
    primary = Color(0xFF2F6B5E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE3DD),
    onPrimaryContainer = Color(0xFF0E2A24),
    secondary = Color(0xFF7A5C2E),
    secondaryContainer = Color(0xFFF6E7C8),
    onSecondaryContainer = Color(0xFF2B1D05),
    background = Color(0xFFFBFAF7),
    onBackground = Color(0xFF1D1C1A),
    surface = Color(0xFFFBFAF7),
    onSurface = Color(0xFF1D1C1A),
    surfaceVariant = Color(0xFFEFEDE7),
    onSurfaceVariant = Color(0xFF5B5953),
    outline = Color(0xFF8A877F),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FCFBF),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1F4F45),
    onPrimaryContainer = Color(0xFFCFE3DD),
    secondary = Color(0xFFE2C38F),
    secondaryContainer = Color(0xFF4A3818),
    onSecondaryContainer = Color(0xFFF6E7C8),
    background = Color(0xFF161816),
    onBackground = Color(0xFFE6E3DC),
    surface = Color(0xFF161816),
    onSurface = Color(0xFFE6E3DC),
    surfaceVariant = Color(0xFF2A2C29),
    onSurfaceVariant = Color(0xFFC4C1B9),
    outline = Color(0xFF8E8C85),
)

@Composable
fun LembarTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
