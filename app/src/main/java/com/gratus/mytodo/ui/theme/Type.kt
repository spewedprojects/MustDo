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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Centralized typography sizing helper objects for clean font size modifications.
 * All styles are increased by +2sp globally.
 */
object AppFontSizes {
    val pico = 10.sp           // originally 8.sp
    val nano = 11.sp           // originally 9.sp
    val micro = 12.sp          // originally 10.sp
    val extraSmall = 13.sp     // originally 11.sp
    val small = 14.sp          // originally 12.sp
    val medium = 15.sp         // originally 13.sp
    val large = 16.sp          // originally 14.sp
    val extraLarge = 18.sp     // originally 16.sp
    val title = 20.sp          // originally 18.sp
    val headline = 26.sp       // originally 24.sp

    // Dynamic zoom scales for HistoryScreen
    fun titleForZoom(level: Int): TextUnit = when (level) {
        1 -> 13.sp   // originally 11
        2 -> 15.sp   // originally 13
        3 -> 17.sp   // originally 15
        4 -> 19.sp   // originally 17
        5 -> 21.sp   // originally 19
        else -> 17.sp
    }

    fun bodyForZoom(level: Int): TextUnit = when (level) {
        1 -> 11.sp   // originally 9
        2 -> 13.sp   // originally 11
        3 -> 13.sp   // originally 11
        4 -> 15.sp   // originally 13
        5 -> 16.sp   // originally 14
        else -> 13.sp
    }
}

/**
 * Material 3 custom typography scaling, where every style is bumped by +2sp.
 */
val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp, // originally 24
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp, // originally 22
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp, // originally 16
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, // originally 14
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp, // originally 16
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp, // originally 14
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, // originally 12
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp, // originally 14
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp, // originally 11
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp, // originally 10
        lineHeight = 16.sp
    )
)
