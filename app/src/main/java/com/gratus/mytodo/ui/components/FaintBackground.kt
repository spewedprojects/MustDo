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

package com.gratus.mytodo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.shiftHueSat

/**
 * FaintBackground draws theme-dependent screen background.
 * For "minimal" theme, it draws extremely soft, beautiful pink and blue blur spheres (blur-3xl equivalent) in light/dark variants.
 * For "colorful" theme, it draws a beautiful, super faint gradient brush to provide premium depth,
 *   with optional hue/saturation shift applied to the gradient stop colors.
 * For "simple" or "system", it renders solid, battery-saving Material background colors.
 *
 * @param colorSchemeType Active color scheme key.
 * @param isDark Whether the current theme is in dark mode. Passed explicitly to avoid
 *   fragile heuristics based on Color channel values which break under hue shifts.
 * @param colorfulHueShift Hue rotation applied to the colorful gradient stops (degrees).
 * @param colorfulSatScale Saturation multiplier applied to the colorful gradient stops.
 */
@Composable
fun FaintBackground(
    colorSchemeType: String,
    isDark: Boolean,
    colorfulHueShift: Float = 0f,
    colorfulSatScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val modifier = when (colorSchemeType) {
        "minimal" -> {
            Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF0F0E13) else Color(0xFFF7F2FA))
        }
        "colorful" -> {
            val baseColors = if (isDark) {
                listOf(
                    Color(0xFF140D1E), // Deep indigo/dark sky
                    Color(0xFF1E112A), // Dark purple Orchid
                    Color(0xFF10121F)  // Deep midnight blue
                )
            } else {
                listOf(
                    Color(0xFFFFF6FA), // Extremely faint pastel rose
                    Color(0xFFF6F0FF), // Faint pastel violet
                    Color(0xFFEDFAF6)  // Faint pastel mint-teal
                )
            }
            val shiftedColors = baseColors.map { it.shiftHueSat(colorfulHueShift, colorfulSatScale) }
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(shiftedColors))
        }
        else -> {
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        }
    }

    Box(modifier = modifier) {
        if (colorSchemeType == "minimal") {
            if (!isDark) {
                // Light mode blurry blobs matching "-top-20 -left-20 w-64 h-64 bg-blue-100/30" and "top-1/2 -right-20 w-80 h-80 bg-pink-100/30"
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    // Top-Left Blue Blur
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3DDBEAFE), Color.Transparent),
                            center = Offset(-60.dp.toPx(), -60.dp.toPx()),
                            radius = 240.dp.toPx()
                        ),
                        radius = 240.dp.toPx(),
                        center = Offset(-60.dp.toPx(), -60.dp.toPx())
                    )
                    // Middle-Right Pink Blur
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3DFCE7F3), Color.Transparent),
                            center = Offset(size.width + 80.dp.toPx(), size.height * 0.55f),
                            radius = 320.dp.toPx()
                        ),
                        radius = 320.dp.toPx(),
                        center = Offset(size.width + 80.dp.toPx(), size.height * 0.55f)
                    )
                }
            } else {
                // Dark mode subtle neon purple/blue space blur
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1A818CF8), Color.Transparent),
                            center = Offset(-40.dp.toPx(), -40.dp.toPx()),
                            radius = 260.dp.toPx()
                        ),
                        radius = 260.dp.toPx(),
                        center = Offset(-40.dp.toPx(), -40.dp.toPx())
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x15F472B6), Color.Transparent),
                            center = Offset(size.width + 60.dp.toPx(), size.height * 0.6f),
                            radius = 340.dp.toPx()
                        ),
                        radius = 340.dp.toPx(),
                        center = Offset(size.width + 60.dp.toPx(), size.height * 0.6f)
                    )
                }
            }
        }
        content()
    }
}
