/*
 * MustDO
 * Copyright (C) 2026 spewedprojects <rkharat98@live.com>
 *
 * This file is part of MustDo Application.
 *
 * MustDo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * See the LICENSE file for details.
 */

package com.gratus.mytodo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * CompositionLocal flag indicating whether the active scheme is "colorful".
 * Used by [dialogContainerColor] to choose a solid overlay background instead of
 * pattern-matching on Color instances (which breaks when hue/sat is shifted).
 */
val LocalIsColorfulScheme = staticCompositionLocalOf { false }

/**
 * Shifts the hue and scales the saturation of this Color in HSV space,
 * preserving the original alpha so that glassmorphism card alphas survive the shift.
 *
 * @param hueDelta Degrees to rotate the hue (positive = clockwise, range typically ±30°).
 * @param satScale Multiplier applied to the saturation channel (range typically 0.7–1.3).
 */
fun Color.shiftHueSat(hueDelta: Float, satScale: Float): Color {
    if (hueDelta == 0f && satScale == 1f) return this
    val hsv = floatArrayOf(0f, 0f, 0f)
    android.graphics.Color.RGBToHSV(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        hsv
    )
    hsv[0] = (hsv[0] + hueDelta + 360f) % 360f
    hsv[1] = (hsv[1] * satScale).coerceIn(0f, 1f)
    val argb = android.graphics.Color.HSVToColor(hsv)
    // Preserve original alpha (critical for semi-transparent card colors)
    return Color(argb).copy(alpha = this.alpha)
}

/**
 * Builds a complete light or dark colorful ColorScheme, applying hue and saturation
 * adjustments uniformly to every color so the inter-color relationships are preserved.
 *
 * @param isDark Whether to build the dark variant.
 * @param hueShift Hue rotation in degrees (default 0 = original curated palette).
 * @param satScale Saturation multiplier (default 1.0 = original curated palette).
 */
fun buildColorfulColorScheme(isDark: Boolean, hueShift: Float, satScale: Float): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary            = ColorfulDarkPrimary.shiftHueSat(hueShift, satScale),
            onPrimary          = SimpleLightBg,
            primaryContainer   = ColorfulDarkPrimaryContainer.shiftHueSat(hueShift, satScale),
            onPrimaryContainer = ColorfulDarkOnPrimaryContainer.shiftHueSat(hueShift, satScale),
            secondary          = ColorfulDarkSecondary.shiftHueSat(hueShift, satScale),
            onSecondary        = SimpleLightBg,
            secondaryContainer = ColorfulDarkSecondaryContainer.shiftHueSat(hueShift, satScale),
            onSecondaryContainer = ColorfulDarkOnSecondaryContainer.shiftHueSat(hueShift, satScale),
            tertiary           = ColorfulDarkTertiary.shiftHueSat(hueShift, satScale),
            onTertiary         = SimpleLightBg,
            tertiaryContainer  = ColorfulDarkTertiaryContainer.shiftHueSat(hueShift, satScale),
            onTertiaryContainer = ColorfulDarkOnTertiaryContainer.shiftHueSat(hueShift, satScale),
            background         = ColorfulDarkBg.shiftHueSat(hueShift, satScale),
            onBackground       = ColorfulDarkOnBg,
            surface            = ColorfulDarkCard.shiftHueSat(hueShift, satScale),
            onSurface          = ColorfulDarkOnBg,
            surfaceVariant     = ColorfulDarkSurfaceVariant.shiftHueSat(hueShift, satScale),
            onSurfaceVariant   = ColorfulDarkOnSurfaceVariant.shiftHueSat(hueShift, satScale),
            outline            = ColorfulDarkOnBg.copy(alpha = 0.2f),
            outlineVariant     = ColorfulDarkOnBg.copy(alpha = 0.1f)
        )
    } else {
        lightColorScheme(
            primary            = ColorfulLightPrimary.shiftHueSat(hueShift, satScale),
            onPrimary          = SimpleLightBg,
            primaryContainer   = ColorfulLightPrimaryContainer.shiftHueSat(hueShift, satScale),
            onPrimaryContainer = ColorfulLightOnPrimaryContainer.shiftHueSat(hueShift, satScale),
            secondary          = ColorfulLightSecondary.shiftHueSat(hueShift, satScale),
            onSecondary        = SimpleLightBg,
            secondaryContainer = ColorfulLightSecondaryContainer.shiftHueSat(hueShift, satScale),
            onSecondaryContainer = ColorfulLightOnSecondaryContainer.shiftHueSat(hueShift, satScale),
            tertiary           = ColorfulLightTertiary.shiftHueSat(hueShift, satScale),
            onTertiary         = SimpleLightBg,
            tertiaryContainer  = ColorfulLightTertiaryContainer.shiftHueSat(hueShift, satScale),
            onTertiaryContainer = ColorfulLightOnTertiaryContainer.shiftHueSat(hueShift, satScale),
            background         = ColorfulLightBg.shiftHueSat(hueShift, satScale),
            onBackground       = ColorfulLightOnBg,
            surface            = ColorfulLightCard.shiftHueSat(hueShift, satScale),
            onSurface          = ColorfulLightOnBg,
            surfaceVariant     = ColorfulLightSurfaceVariant.shiftHueSat(hueShift, satScale),
            onSurfaceVariant   = ColorfulLightOnSurfaceVariant.shiftHueSat(hueShift, satScale),
            outline            = ColorfulLightOnBg.copy(alpha = 0.1f),
            outlineVariant     = ColorfulLightOnBg.copy(alpha = 0.05f)
        )
    }
}

/**
 * Computes the solid-background dialog color for the colorful scheme.
 * Applies the same hue/sat shift to the opaque card color (Card2 variants).
 */
fun colorfulDialogContainerColor(isDark: Boolean, hueShift: Float, satScale: Float): Color =
    if (isDark) ColorfulDarkCard2.shiftHueSat(hueShift, satScale)
    else ColorfulLightCard2.shiftHueSat(hueShift, satScale)

/**
 * Application-wide CompositionLocal carrying the solid dialog background color
 * resolved for the current theme.  Defaults to Color.Unspecified so that
 * components not wrapped by SoftTodoTheme degrade gracefully.
 */
val LocalDialogContainerColor = staticCompositionLocalOf { Color.Unspecified }

@Composable
fun SoftTodoTheme(
    themeMode: String = "auto",
    colorSchemeType: String = "minimal",
    colorfulHueShift: Float = 0f,
    colorfulSatScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "light" -> false
        "dark"  -> true
        else    -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val isColorful = colorSchemeType == "colorful"

    val colorScheme = when (colorSchemeType) {
        "minimal" -> {
            if (isDark) {
                darkColorScheme(
                    primary = MinimalDarkAccent,
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF1E1B4B),
                    onPrimaryContainer = Color(0xFFC7D2FE),
                    secondary = MinimalDarkAccent,
                    onSecondary = Color.Black,
                    secondaryContainer = Color(0xFF2E234A),
                    onSecondaryContainer = Color(0xFFDDD6FE),
                    tertiary = Color(0xFFF472B6),
                    onTertiary = Color.Black,
                    tertiaryContainer = Color(0xFF4C1D35),
                    onTertiaryContainer = Color(0xFFFBCFE8),
                    background = MinimalDarkBg,
                    onBackground = MinimalDarkText,
                    surface = MinimalDarkCard,
                    onSurface = MinimalDarkText,
                    surfaceVariant = Color(0xFF1E1A29),
                    onSurfaceVariant = MinimalDarkText.copy(alpha = 0.6f),
                    outline = Color(0xFF1E293B)
                )
            } else {
                lightColorScheme(
                    primary = MinimalLightAccent,
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFE0E7FF),
                    onPrimaryContainer = Color(0xFF1E1B4B),
                    secondary = MinimalLightAccent,
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFEDE9FE),
                    onSecondaryContainer = Color(0xFF312E81),
                    tertiary = Color(0xFFEC4899),
                    onTertiary = Color.White,
                    tertiaryContainer = Color(0xFFFCE7F3),
                    onTertiaryContainer = Color(0xFF831843),
                    background = MinimalLightBg,
                    onBackground = MinimalLightText,
                    surface = MinimalLightCard2,
                    onSurface = MinimalLightText,
                    surfaceVariant = Color(0xFFF1EDF7),
                    onSurfaceVariant = MinimalLightText.copy(alpha = 0.6f),
                    outline = Color(0xFFE2E8F0)
                )
            }
        }
        "simple" -> {
            if (isDark) {
                darkColorScheme(
                    primary = SimpleDarkAccent,
                    onPrimary = SimpleDarkBg,
                    primaryContainer = Color(0xFF1C1C1E),
                    onPrimaryContainer = Color(0xFFFFFFFF),
                    secondary = SimpleDarkText.copy(alpha = 0.8f),
                    onSecondary = SimpleDarkBg,
                    secondaryContainer = Color(0xFF2C2C2E),
                    onSecondaryContainer = Color(0xFFFFFFFF),
                    tertiary = SimpleDarkText.copy(alpha = 0.6f),
                    onTertiary = SimpleDarkBg,
                    tertiaryContainer = Color(0xFF3A3A3C),
                    onTertiaryContainer = Color(0xFFFFFFFF),
                    background = SimpleDarkBg,
                    onBackground = SimpleDarkText,
                    surface = SimpleDarkBg,
                    onSurface = SimpleDarkText,
                    surfaceVariant = Color(0xFF1C1C1E),
                    onSurfaceVariant = SimpleDarkText.copy(alpha = 0.7f),
                    inverseSurface = SimpleDarkText,
                    inverseOnSurface = SimpleDarkBg,
                    outline = SimpleDarkText.copy(alpha = 0.3f)
                )
            } else {
                lightColorScheme(
                    primary = SimpleLightAccent,
                    onPrimary = SimpleLightBg,
                    primaryContainer = Color(0xFFE5E5EA),
                    onPrimaryContainer = Color(0xFF000000),
                    secondary = SimpleLightText.copy(alpha = 0.8f),
                    onSecondary = SimpleLightBg,
                    secondaryContainer = Color(0xFFF2F2F7),
                    onSecondaryContainer = Color(0xFF000000),
                    tertiary = SimpleLightText.copy(alpha = 0.6f),
                    onTertiary = SimpleLightBg,
                    tertiaryContainer = Color(0xFFEAEAEA),
                    onTertiaryContainer = Color(0xFF000000),
                    background = SimpleLightBg,
                    onBackground = SimpleLightText,
                    surface = SimpleLightCard,
                    onSurface = SimpleLightText,
                    surfaceVariant = Color(0xFFF2F2F7),
                    onSurfaceVariant = SimpleLightText.copy(alpha = 0.7f),
                    inverseSurface = SimpleLightText,
                    inverseOnSurface = SimpleLightBg,
                    outline = SimpleLightText.copy(alpha = 0.3f)
                )
            }
        }
        "colorful" -> buildColorfulColorScheme(isDark, colorfulHueShift, colorfulSatScale)
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

    // Resolve the correct solid dialog background once and expose it via CompositionLocal
    val dialogBg = if (isColorful) {
        colorfulDialogContainerColor(isDark, colorfulHueShift, colorfulSatScale)
    } else {
        colorScheme.surface
    }

    CompositionLocalProvider(
        LocalIsColorfulScheme provides isColorful,
        LocalDialogContainerColor provides dialogBg
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Returns a solid background suitable for dialogs and dropdown menus.
 * For the colorful scheme, this is the shifted opaque Card2 color;
 * for all other schemes it is [ColorScheme.surface].
 *
 * Uses [LocalDialogContainerColor] which is set by [SoftTodoTheme], so it correctly
 * reflects any active hue/saturation shift without fragile Color identity matching.
 */
val ColorScheme.dialogContainerColor: Color
    @Composable get() = LocalDialogContainerColor.current.takeIf { it != Color.Unspecified }
        ?: this.surface
