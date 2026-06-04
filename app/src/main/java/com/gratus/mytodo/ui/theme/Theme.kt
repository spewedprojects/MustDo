package com.gratus.mytodo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Standard light/dark fallback dynamic styling palletes
private val FallbackDarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val FallbackLightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun SoftTodoTheme(
    themeMode: String = "auto",
    colorSchemeType: String = "minimal",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when (colorSchemeType) {
        "minimal" -> {
            if (isDark) {
                darkColorScheme(
                    primary = MinimalDarkAccent,
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF1E1B4B),
                    onPrimaryContainer = Color(0xFFC7D2FE),
                    background = MinimalDarkBg,
                    onBackground = MinimalDarkText,
                    surface = MinimalDarkCard,
                    onSurface = MinimalDarkText,
                    onSurfaceVariant = MinimalDarkText.copy(alpha = 0.6f),
                    secondary = MinimalDarkAccent,
                    outline = Color(0xFF1E293B)
                )
            } else {
                lightColorScheme(
                    primary = MinimalLightAccent,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE0E7FF),
                    onPrimaryContainer = Color(0xFF1E1B4B),
                    background = MinimalLightBg,
                    onBackground = MinimalLightText,
                    surface = MinimalLightCard,
                    onSurface = MinimalLightText,
                    onSurfaceVariant = MinimalLightText.copy(alpha = 0.6f),
                    secondary = MinimalLightAccent,
                    outline = Color(0xFFE2E8F0)
                )
            }
        }
        "simple" -> {
            if (isDark) {
                darkColorScheme(
                    primary = SimpleDarkAccent,
                    onPrimary = SimpleDarkBg,
                    background = SimpleDarkBg,
                    onBackground = SimpleDarkText,
                    surface = SimpleDarkBg,
                    onSurface = SimpleDarkText,
                    secondary = SimpleDarkText.copy(alpha = 0.6f),
                    outline = SimpleDarkText.copy(alpha = 0.3f)
                )
            } else {
                lightColorScheme(
                    primary = SimpleLightAccent,
                    onPrimary = SimpleLightBg,
                    background = SimpleLightBg,
                    onBackground = SimpleLightText,
                    surface = SimpleLightCard,
                    onSurface = SimpleLightText,
                    secondary = SimpleLightText.copy(alpha = 0.6f),
                    outline = SimpleLightText.copy(alpha = 0.3f)
                )
            }
        }
        "colorful" -> {
            if (isDark) {
                darkColorScheme(
                    primary = ColorfulDarkPrimary,
                    onPrimary = SimpleLightBg,
                    secondary = ColorfulDarkSecondary,
                    tertiary = ColorfulDarkTertiary,
                    background = ColorfulDarkBg,
                    onBackground = ColorfulDarkOnBg,
                    surface = ColorfulDarkCard,
                    onSurface = ColorfulDarkOnBg,
                    outline = ColorfulDarkOnBg.copy(alpha = 0.2f)
                )
            } else {
                lightColorScheme(
                    primary = ColorfulLightPrimary,
                    onPrimary = SimpleLightBg,
                    secondary = ColorfulLightSecondary,
                    tertiary = ColorfulLightTertiary,
                    background = ColorfulLightBg,
                    onBackground = ColorfulLightOnBg,
                    surface = ColorfulLightCard,
                    onSurface = ColorfulLightOnBg,
                    outline = ColorfulLightOnBg.copy(alpha = 0.1f)
                )
            }
        }
        "system" -> {
            // Dynamic theme support (Android 12+) or traditional light/dark M3
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) FallbackDarkColorScheme else FallbackLightColorScheme
            }
        }
        else -> FallbackLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
