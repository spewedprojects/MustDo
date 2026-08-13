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

package com.gratus.mytodo.ui.screens

import com.gratus.mytodo.ui.components.stats.CompletionRateCard
import com.gratus.mytodo.ui.components.stats.ConsistencyCard
import com.gratus.mytodo.ui.components.stats.WeeklyChartCard

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.ui.DailyStats
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.StatsData
import com.gratus.mytodo.ui.theme.SoftTodoTheme

/**
 * Statistics dashboard displaying task metrics. Dynamically shifts structure between
 * portrait and landscape orientations to optimize screen space without vertical scrolling.
 */
@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    colorSchemeType: String
) {
    val stats by viewModel.statsFlow.collectAsState(initial = StatsData(0, 0, 0, 0, emptyList()))
    StatsScreenContent(stats = stats)
}

@Composable
fun StatsScreenContent(stats: StatsData) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CompletionRateCard(stats = stats, modifier = Modifier.weight(1f).fillMaxWidth())
                ConsistencyCard(stats = stats, modifier = Modifier.weight(1f).fillMaxWidth())
            }
            WeeklyChartCard(stats = stats, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CompletionRateCard(stats = stats, modifier = Modifier.weight(1f).fillMaxHeight())
                ConsistencyCard(stats = stats, modifier = Modifier.weight(1f).fillMaxHeight())
            }
            WeeklyChartCard(stats = stats, modifier = Modifier.fillMaxWidth().weight(2f))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Stats Screen - Navigable System UI")
@Composable
fun StatsScreenNavigablePreview() {
    val sampleStats = StatsData(
        totalTasks = 12,
        completedTasks = 8,
        completionRate = 67,
        currentStreak = 4,
        weeklyHistory = listOf(
            DailyStats("Mon", 2, 3),
            DailyStats("Tue", 1, 2),
            DailyStats("Wed", 3, 3),
            DailyStats("Thu", 0, 1),
            DailyStats("Fri", 2, 4),
            DailyStats("Sat", 5, 5),
            DailyStats("Sun", 4, 6)
        )
    )
    SoftTodoTheme {
        StatsScreenContent(stats = sampleStats)
    }
}

@Preview(showBackground = true, name = "Stats Screen - Portrait")
@Composable
fun StatsScreenPreview() {
    val sampleStats = StatsData(
        totalTasks = 12,
        completedTasks = 8,
        completionRate = 67,
        currentStreak = 4,
        weeklyHistory = listOf(
            DailyStats("Mon", 2, 3),
            DailyStats("Tue", 1, 2),
            DailyStats("Wed", 3, 3),
            DailyStats("Thu", 0, 1),
            DailyStats("Fri", 2, 4),
            DailyStats("Sat", 5, 5),
            DailyStats("Sun", 4, 6)
        )
    )
    SoftTodoTheme {
        StatsScreenContent(stats = sampleStats)
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun StatsScreenLandscapePreview() {
    val sampleStats = StatsData(
        totalTasks = 12,
        completedTasks = 8,
        completionRate = 67,
        currentStreak = 4,
        weeklyHistory = listOf(
            DailyStats("Mon", 2, 3),
            DailyStats("Tue", 1, 2),
            DailyStats("Wed", 3, 3),
            DailyStats("Thu", 0, 1),
            DailyStats("Fri", 2, 4),
            DailyStats("Sat", 5, 5),
            DailyStats("Sun", 4, 6)
        )
    )
    SoftTodoTheme {
        StatsScreenContent(stats = sampleStats)
    }
}
