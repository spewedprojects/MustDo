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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Priority Colors (Red to Yellow, 1 to 4)
val PriorityRed = Color(0xFFE57373)      // Priority 1 - Critical / Urgent
val PriorityOrange = Color(0xFFFFB74D)   // Priority 2 - High
val PriorityAmber = Color(0xFFFFF176)    // Priority 3 - Medium
val PriorityYellow = Color(0xFFE8F5E9)   // Priority 4 - Low (soft calm mint-yellow / light pastel)

// Simple Theme Color Palette (Black & White)
val SimpleLightBg = Color(0xFFFFFFFF)
val SimpleLightText = Color(0xFF000000)
val SimpleLightAccent = Color(0xFF000000)
val SimpleLightCard = Color(0xFFFFFFFF)

val SimpleDarkBg = Color(0xFF000000)
val SimpleDarkText = Color(0xFFFFFFFF)
val SimpleDarkAccent = Color(0xFFFFFFFF)
val SimpleDarkCard = Color(0xFF000000)

// Colorful Theme Color Palette (Soft Pastels with faint radial canvas background)
val ColorfulLightBg = Color(0xFFFFF9FB)          // Soft roseate/white base
val ColorfulLightOnBg = Color(0xFF2C1A2E)
val ColorfulLightCard = Color(0x99FFFFFF)        // Semi-transparent for layered glassmorphism look
val ColorfulLightCard2 = Color(0xFFF4F5FD)       // Opaque but not true white
val ColorfulLightPrimary = Color(0xFFEC407A)     // Pastel Rose
val ColorfulLightPrimaryContainer = Color(0xFFFCE4EC) // Soft pastel rose container
val ColorfulLightOnPrimaryContainer = Color(0xFF880E4F) // Deep rose text
val ColorfulLightSecondary = Color(0xFFAB47BC)   // Pastel Orchid Purple
val ColorfulLightSecondaryContainer = Color(0xFFF3E5F5) // Soft pastel orchid container
val ColorfulLightOnSecondaryContainer = Color(0xFF4A148C) // Deep orchid text
val ColorfulLightTertiary = Color(0xFF26A69A)    // Pastel Teal
val ColorfulLightTertiaryContainer = Color(0xFFE0F2F1) // Soft pastel teal container
val ColorfulLightOnTertiaryContainer = Color(0xFF004D40) // Deep teal text
val ColorfulLightSurfaceVariant = Color(0xFFF5EDF4) // Tinted pastel background for chips/inputs
val ColorfulLightOnSurfaceVariant = Color(0xFF5C4761) // Muted plum text

val ColorfulDarkBg = Color(0xFF140D1E)           // Deep indigo/night sky base
val ColorfulDarkOnBg = Color(0xFFE8E1F0)
val ColorfulDarkCard = Color(0x334E3470)         // Layered glow cards
val ColorfulDarkCard2 = Color(0xFF160D1E)        // Solid color
val ColorfulDarkPrimary = Color(0xFFF06292)      // Pastel glow rose
val ColorfulDarkPrimaryContainer = Color(0xFF4A152E) // Deep rose/wine container
val ColorfulDarkOnPrimaryContainer = Color(0xFFFFD1DC) // Light pastel rose text
val ColorfulDarkSecondary = Color(0xFFBA68C8)    // Pastel glow orchid purple
val ColorfulDarkSecondaryContainer = Color(0xFF381545) // Deep plum/orchid container
val ColorfulDarkOnSecondaryContainer = Color(0xFFE8C5F5) // Light pastel orchid text
val ColorfulDarkTertiary = Color(0xFF4DB6AC)     // Pastel glow mint-teal
val ColorfulDarkTertiaryContainer = Color(0xFF0D3834) // Deep teal container
val ColorfulDarkOnTertiaryContainer = Color(0xFF80CBC4) // Light pastel teal text
val ColorfulDarkSurfaceVariant = Color(0xFF241630) // Deep indigo-tinted surface for chips/inputs
val ColorfulDarkOnSurfaceVariant = Color(0xFFBCAEC4) // Muted lilac text

// Minimalist Theme Color Palette (Clean Minimal lavender bg with semi-transparent card overlays)
val MinimalLightBg = Color(0xFFF7F2FA)
val MinimalLightText = Color(0xFF1E293B)          // Slate 800
val MinimalLightAccent = Color(0xFF6366F1)        // Indigo 500
val MinimalLightCard = Color(0x99FFFFFF)          // bg-white/60 layer over lavender

val MinimalLightCard2 = Color(0xFFFCFAFD)        // bg-white/60 layer over lavender - but solid
val MinimalLightCardBorder = Color(0x66FFFFFF)    // border-white/40

val MinimalDarkBg = Color(0xFF0F0E13)
val MinimalDarkText = Color(0xFFF1F5F9)           // Slate 100
val MinimalDarkAccent = Color(0xFF818CF8)         // Indigo 400
val MinimalDarkCard = Color(0xFF0F0E13)           // bg-dark/40 layer over midnight-lilac
val MinimalDarkCardBorder = Color(0x22FFFFFF)     // border-white/10

val MinimalP1BagBg = Color(0xFFFFE4E6)            // bg-rose-100
val MinimalP1Text = Color(0xFFE11D48)             // text-rose-600
val MinimalP1Border = Color(0xFFFECDD3)           // border-rose-200

val MinimalP2BagBg = Color(0xFFFFEDD5)            // bg-orange-100
val MinimalP2Text = Color(0xFFEA580C)             // text-orange-600
val MinimalP2Border = Color(0xFFFED7AA)           // border-orange-200

val MinimalP3BagBg = Color(0xFFFEF3C7)            // bg-amber-100
val MinimalP3Text = Color(0xFFD97706)             // text-amber-600
val MinimalP3Border = Color(0xFFFDE68A)           // border-amber-200

val MinimalP4BagBg = Color(0xFFF1F5F9)            // bg-slate-100
val MinimalP4Text = Color(0xFF475569)             // text-slate-600
val MinimalP4Border = Color(0xFFE2E8F0)           // border-slate-200

// Default Fallback / Monet Palette (Material Purple)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Helper models and style provider function for Minimal priority level badges
class PriorityThemeBadgeColors(val containerColor: Color, val contentColor: Color, val borderColor: Color)

fun getMinimalPriorityColors(priority: Int, isCompleted: Boolean, isDark: Boolean): PriorityThemeBadgeColors {
    if (isCompleted) {
        return if (isDark) {
            PriorityThemeBadgeColors(
                containerColor = Color(0x19FFFFFF),
                contentColor = Color(0xFF64748B),
                borderColor = Color(0x11FFFFFF)
            )
        } else {
            PriorityThemeBadgeColors(
                containerColor = Color(0xFFE2E8F0),
                contentColor = Color(0xFF94A3B8),
                borderColor = Color(0x66E2E8F0)
            )
        }
    } else {
        if (isDark) {
            return when (priority) {
                1 -> PriorityThemeBadgeColors(
                    containerColor = Color(0x33E11D48),
                    contentColor = Color(0xFFFB7185),
                    borderColor = Color(0x66FB7185)
                )
                2 -> PriorityThemeBadgeColors(
                    containerColor = Color(0x33EA580C),
                    contentColor = Color(0xFFFDBA74),
                    borderColor = Color(0x66FDBA74)
                )
                3 -> PriorityThemeBadgeColors(
                    containerColor = Color(0x33D97706),
                    contentColor = Color(0xFFFDE047),
                    borderColor = Color(0x66FDE047)
                )
                else -> PriorityThemeBadgeColors(
                    containerColor = Color(0x33475569),
                    contentColor = Color(0xFF94A3B8),
                    borderColor = Color(0x6694A3B8)
                )
            }
        } else {
            return when (priority) {
                1 -> PriorityThemeBadgeColors(
                    containerColor = MinimalP1BagBg,
                    contentColor = MinimalP1Text,
                    borderColor = MinimalP1Border
                )
                2 -> PriorityThemeBadgeColors(
                    containerColor = MinimalP2BagBg,
                    contentColor = MinimalP2Text,
                    borderColor = MinimalP2Border
                )
                3 -> PriorityThemeBadgeColors(
                    containerColor = MinimalP3BagBg,
                    contentColor = MinimalP3Text,
                    borderColor = MinimalP3Border
                )
                else -> PriorityThemeBadgeColors(
                    containerColor = MinimalP4BagBg,
                    contentColor = MinimalP4Text,
                    borderColor = MinimalP4Border
                )
            }
        }
    }
}

/**
 * Resolves color coding accents matching default and custom category titles.
 * Returns monochromatic palette colors when colorSchemeType is "simple".
 */
@Composable
fun getCategoryAccentColor(category: String, colorSchemeType: String = "", isDark: Boolean = false): Color {
    if (colorSchemeType == "simple") {
        return if (isDark) SimpleDarkAccent else SimpleLightAccent
    }
    val lower = category.lowercase().trim()
    return when {
        lower.contains("sticky") -> MaterialTheme.colorScheme.primary
        lower.contains("work") || lower.contains("job") || lower.contains("office") || lower.contains("meet") || lower.contains("project") -> Color(0xFFE91E63) // Pink/Rose
        lower.contains("personal") || lower.contains("home") || lower.contains("self") || lower.contains("me") || lower.contains("private") -> Color(0xFF2196F3) // Blue
        lower.contains("errand") || lower.contains("shop") || lower.contains("buy") || lower.contains("grocer") || lower.contains("store") || lower.contains("market") -> Color(0xFF4CAF50) // Green
        lower.contains("gym") || lower.contains("workout") || lower.contains("exercise") || lower.contains("run") || lower.contains("fit") || lower.contains("sport") || lower.contains("fitness") || lower.contains("dumbbell") -> Color(0xFF9C27B0) // Purple
        lower.contains("health") || lower.contains("doctor") || lower.contains("hospital") || lower.contains("med") || lower.contains("medicine") || lower.contains("favorite") -> Color(0xFFFF5722) // Orange
        lower.contains("learn") || lower.contains("study") || lower.contains("book") || lower.contains("school") || lower.contains("class") || lower.contains("course") || lower.contains("read") -> Color(0xFFFFC107) // Amber/Yellow
        else -> Color(0xFF673AB7) // Indigo/default
    }
}
