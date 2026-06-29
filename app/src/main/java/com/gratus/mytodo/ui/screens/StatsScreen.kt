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

@Composable
fun CompletionRateCard(stats: StatsData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Completion Rate",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                val strokeColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                
                Canvas(modifier = Modifier.size(64.dp)) {
                    // Track circle
                    drawCircle(
                        color = trackColor,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 6.dp.toPx())
                    )
                    // Content sweeps progress
                    val sweepAngle = 360f * (stats.completionRate.toFloat() / 100f).coerceIn(0f, 1f)
                    drawArc(
                        color = strokeColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }

                Text(
                    text = "${stats.completionRate}%",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "${stats.completedTasks} of ${stats.totalTasks} Done",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ConsistencyCard(stats: StatsData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Consistency",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Streak Flame graphics
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (stats.currentStreak > 0) Color(0xFFFFECEB) 
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = "Streaks counts",
                    tint = if (stats.currentStreak > 0) Color(0xFFFF5722) else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${stats.currentStreak} Day Streak",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (stats.currentStreak > 0) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Keep completing tasks!",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun WeeklyChartCard(stats: StatsData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.testTag("weekly_stats_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chart Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Last 7 Days Outline",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Completed tasks vs total schedule",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Simple color cues legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done", fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primaryContainer))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Total", fontSize = 10.sp)
                    }
                }
            }

            if (stats.weeklyHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Unable to render weekly metrics history", fontSize = 11.sp, color = Color.Gray)
                }
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)

                // Drawing custom Canvas bar chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 12.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val paddingLeft = 30f
                    val paddingBottom = 40f
                    val chartWidth = canvasWidth - paddingLeft
                    val chartHeight = canvasHeight - paddingBottom

                    // Find maximum height scale (at least 5 to prevent division anomalies)
                    val maxTotal = stats.weeklyHistory.maxOfOrNull { it.total } ?: 5
                    val maxScale = maxOf(maxTotal, 5)

                    val barGroupWidth = chartWidth / 7f
                    val individualBarWidth = barGroupWidth * 0.3f

                    // Draw Grid Horizontal Lines
                    val gridCount = 4
                    for (i in 0..gridCount) {
                        val y = chartHeight * (i.toFloat() / gridCount)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(paddingLeft, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Cycle through individual weekly elements and draw bars
                    stats.weeklyHistory.forEachIndexed { index, daily ->
                        val xPos = paddingLeft + (index * barGroupWidth)

                        // Compute heights proportionately
                        val totalBarHeight = if (daily.total > 0) {
                            (daily.total.toFloat() / maxScale) * chartHeight
                        } else {
                            4.dp.toPx() // Minimum faint indicator for 0 tasks
                        }

                        val completedBarHeight = if (daily.completed > 0) {
                            (daily.completed.toFloat() / maxScale) * chartHeight
                        } else {
                            0f
                        }

                        // Draw Total tasks bar (light colored container background)
                        drawRoundRect(
                            color = primaryContainerColor,
                            topLeft = Offset(xPos + individualBarWidth / 2, chartHeight - totalBarHeight),
                            size = Size(individualBarWidth, totalBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Draw Completed task bar side-by-side / overlayed
                        if (completedBarHeight > 0) {
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(xPos + individualBarWidth * 1.6f, chartHeight - completedBarHeight),
                                size = Size(individualBarWidth, completedBarHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }

                // Labels mapping under canvas row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    stats.weeklyHistory.forEach { daily ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = daily.dateLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${daily.completed}/${daily.total}",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
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
