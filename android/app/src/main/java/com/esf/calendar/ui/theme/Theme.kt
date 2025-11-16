package com.esf.calendar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Thème dark futuriste (principal)
 */
private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = DeepBlue,
    onPrimaryContainer = CyberBlue,

    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = DeepPurple,
    onSecondaryContainer = FuturaPink,

    tertiary = ElectricGreen,
    onTertiary = SpaceBlack,
    tertiaryContainer = NeonYellow,
    onTertiaryContainer = Color.White,

    background = SpaceBlack,
    onBackground = TextPrimary,

    surface = GlassSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassSecondary,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = Color.White
)

/**
 * Thème light (optionnel, pour les utilisateurs qui préfèrent le mode clair)
 */
private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = CyberBlue,
    onPrimaryContainer = DeepBlue,

    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = FuturaPink,
    onSecondaryContainer = DeepPurple,

    tertiary = ElectricGreen,
    onTertiary = Color.White,
    tertiaryContainer = NeonYellow,
    onTertiaryContainer = SpaceBlack,

    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF1A1C1E),

    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7E8EC),
    onSurfaceVariant = Color(0xFF44464F),

    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ESFCalendarTheme(
    darkTheme: Boolean = true, // Forcer le dark mode par défaut pour le look futuriste
    dynamicColor: Boolean = false, // Désactiver les couleurs dynamiques
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
