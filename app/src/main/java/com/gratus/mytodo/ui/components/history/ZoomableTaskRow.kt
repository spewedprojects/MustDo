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

package com.gratus.mytodo.ui.components.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.components.home.getPriorityBoxColor
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.MinimalDarkCardBorder
import com.gratus.mytodo.ui.theme.MinimalLightCardBorder
import com.gratus.mytodo.ui.theme.PriorityThemeBadgeColors
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.getMinimalPriorityColors

private val sampleHistoryTasks = listOf(
    Task(id = 1, title = "Finish Project Proposal", description = "Finalize the budget and timeline", priority = 1, dateAdded = "2023-10-25", isCompleted = true),
    Task(id = 2, title = "Grocery Shopping", description = "Milk, Eggs, Bread, Fruits", priority = 2, dateAdded = "2023-10-25", isCompleted = true),
    Task(id = 3, title = "Gym Workout", description = "Leg day", priority = 3, dateAdded = "2023-10-24", isCompleted = false)
)

/**
 * Zoomable item representing a row. Spacings and fonts respond to zoom levels.
 */
@Composable
fun ZoomableTaskRow(task: Task, zoomLevel: Int, colorSchemeType: String) {
    val isCompleted = task.isCompleted
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    // Zoom dynamic attributes mapping
    val paddingValues = when (zoomLevel) {
        1 -> PaddingValues(vertical = 4.dp, horizontal = 8.dp)
        2 -> PaddingValues(vertical = 6.dp, horizontal = 10.dp)
        3 -> PaddingValues(vertical = 10.dp, horizontal = 12.dp)
        else -> PaddingValues(10.dp)
    }

    val titleSize = AppFontSizes.titleForZoom(zoomLevel)
    val bodySize = AppFontSizes.bodyForZoom(zoomLevel)

    val cardShape = when (zoomLevel) {
        1 -> RoundedCornerShape(6.dp)
        2 -> RoundedCornerShape(10.dp)
        else -> RoundedCornerShape(14.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                if (colorSchemeType == "minimal") {
                    if (isDark) Color(0x15FFFFFF) else Color(0x33B0AAB9)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                }
            } else {
                if (colorSchemeType == "minimal") {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            }
        ),
        border = if (colorSchemeType == "minimal") {
            if (isCompleted) {
                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder)
            }
        } else {
            null
        },
        shape = cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Elegant circular checkbox structure
            Box(
                modifier = Modifier
                    .size(if (zoomLevel == 1) 14.dp else 18.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                    .then(
                        if (!isCompleted) {
                            Modifier.border(
                                1.5.dp,
                                MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(if (zoomLevel == 1) 9.dp else 11.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = titleSize,
                        maxLines = if (zoomLevel == 1) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Display tiny date if not in Grouped mode
                    Text(
                        text = "• " + task.dateAdded,
                        fontSize = if (zoomLevel == 1) AppFontSizes.pico else AppFontSizes.micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Show descriptions if user is not fully zoomed out
                if (zoomLevel > 1 && task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parseStyledDescription(task.description),
                        fontSize = bodySize,
                        maxLines = if (zoomLevel == 2) 1 else 4,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Small priority box matching the Home design
            val badgeStyle = if (colorSchemeType == "minimal") {
                getMinimalPriorityColors(task.priority, isCompleted, isDark)
            } else {
                val containerCol = getPriorityBoxColor(task.priority, isCompleted)
                val textCol = if (task.priority == 4 || isCompleted) Color.DarkGray else Color.White
                val borderCol = Color.Transparent
                PriorityThemeBadgeColors(containerCol, textCol, borderCol)
            }

            Box(
                modifier = Modifier
                    .size(if (zoomLevel == 1) 16.dp else 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeStyle.containerColor)
                    .then(
                        if (colorSchemeType == "minimal") {
                            Modifier.border(1.dp, badgeStyle.borderColor, RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = task.priority.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (zoomLevel == 1) AppFontSizes.pico else AppFontSizes.micro,
                    color = badgeStyle.contentColor
                )
            }
        }
    }
}

@Preview(showBackground = false, name = "Zoomable Task Row - Level 1 (Compact)")
@Composable
fun ZoomableTaskRowLevel1Preview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
        Box(modifier = Modifier.padding(16.dp)) {
            ZoomableTaskRow(
                task = sampleHistoryTasks[0],
                zoomLevel = 1,
                colorSchemeType = "minimal"
            )
        }
    }
}

@Preview(showBackground = false, name = "Zoomable Task Row - Level 2 (Medium)")
@Composable
fun ZoomableTaskRowLevel2Preview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
        Box(modifier = Modifier.padding(16.dp)) {
            ZoomableTaskRow(
                task = sampleHistoryTasks[0],
                zoomLevel = 2,
                colorSchemeType = "minimal"
            )
        }
    }
}

@Preview(showBackground = false, name = "Zoomable Task Row - Level 3 (Detailed)")
@Composable
fun ZoomableTaskRowLevel3Preview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
        Box(modifier = Modifier.padding(16.dp)) {
            ZoomableTaskRow(
                task = sampleHistoryTasks[0],
                zoomLevel = 3,
                colorSchemeType = "minimal"
            )
        }
    }
}
