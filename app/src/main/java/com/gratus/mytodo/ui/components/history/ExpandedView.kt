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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.SubTask
import com.gratus.mytodo.ui.components.dialogs.getCategoryIcon
//import com.gratus.mytodo.ui.components.home.getCategoryAccentColor
import com.gratus.mytodo.ui.components.home.getPriorityBoxColor
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.MinimalDarkCardBorder
import com.gratus.mytodo.ui.theme.MinimalLightCardBorder
import com.gratus.mytodo.ui.theme.PriorityThemeBadgeColors
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.getCategoryAccentColor
import com.gratus.mytodo.ui.theme.getMinimalPriorityColors
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Level 3: Expanded View (Groups tasks by exact date, shows continuous vertical details stream)
 */
@Composable
fun ExpandedView(
    tasks: List<Task>,
    stickyTasks: List<Task> = emptyList(),
    colorSchemeType: String,
    onNavigateToHomeDate: ((Calendar, Int?) -> Unit)? = null,
    isDoubleColumn: Boolean = false
) {
    val grouped = remember(tasks) { tasks.groupBy { it.dateAdded } }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        grouped.keys.toList().sortedDescending().forEach { dateStr ->
            val dateObj = DateTimeUtils.parseDbDate(dateStr) ?: Date()
            val cardCal = Calendar.getInstance().apply { time = dateObj }
            val groupTasks = grouped[dateStr] ?: emptyList()
            
            item(key = dateStr) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = when (colorSchemeType) {
                        "simple" -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        "minimal" -> {
                            val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                            BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
                        }
                        else -> null
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dayStickyTasks = remember(groupTasks) {
                            groupTasks.filter { it.category?.equals("Sticky", ignoreCase = true) == true }
                        }
                        val dayRegularTasks = remember(groupTasks) {
                            groupTasks.filter { it.category?.equals("Sticky", ignoreCase = true) != true }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateTimeUtils.formatHistoryGroup(dateObj),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (dayStickyTasks.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${dayStickyTasks.size} Sticky",
                                        fontSize = AppFontSizes.extraSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        
                        if (dayStickyTasks.isNotEmpty()) {
                            dayStickyTasks.forEach { stickyTask ->
                                ExpandedTaskRow(
                                    task = stickyTask,
                                    targetDate = cardCal,
                                    colorSchemeType = colorSchemeType,
                                    onNavigateToHomeDate = onNavigateToHomeDate,
                                    showDescription = !isDoubleColumn
                                )
                            }
                        }
                        
                        if (isDoubleColumn) {
                            val col1Tasks = dayRegularTasks.filterIndexed { index, _ -> index % 2 == 0 }
                            val col2Tasks = dayRegularTasks.filterIndexed { index, _ -> index % 2 == 1 }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    col1Tasks.forEach { task ->
                                        ExpandedTaskRow(
                                            task = task,
                                            targetDate = cardCal,
                                            colorSchemeType = colorSchemeType,
                                            onNavigateToHomeDate = onNavigateToHomeDate,
                                            showDescription = false
                                        )
                                    }
                                }
                                if (col2Tasks.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        col2Tasks.forEach { task ->
                                            ExpandedTaskRow(
                                                task = task,
                                                targetDate = cardCal,
                                                colorSchemeType = colorSchemeType,
                                                onNavigateToHomeDate = onNavigateToHomeDate,
                                                showDescription = false
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            dayRegularTasks.forEach { task ->
                                ExpandedTaskRow(
                                    task = task,
                                    targetDate = cardCal,
                                    colorSchemeType = colorSchemeType,
                                    onNavigateToHomeDate = onNavigateToHomeDate,
                                    showDescription = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandedTaskRow(
    task: Task,
    targetDate: Calendar? = null,
    colorSchemeType: String,
    onNavigateToHomeDate: ((Calendar, Int?) -> Unit)? = null,
    showDescription: Boolean = true
) {
    val isCompleted = task.isCompleted
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onNavigateToHomeDate != null) {
                    Modifier.clickable {
                        val cal = targetDate ?: Calendar.getInstance().apply {
                            time = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
                        }
                        onNavigateToHomeDate(cal, task.id)
                    }
                } else Modifier
            ),
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
                BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
            } else {
                BorderStroke(1.dp, if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder)
            }
        } else {
            null
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primaryContainer 
                            else Color.Transparent
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (task.category != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            val annotatedText = buildAnnotatedString {
                                // Inline category icon first
                                appendInlineContent("categoryIcon", "[icon] ")
                                append(" ")
                                // Then the task title text
                                append(task.title)
                            }

                            val inlineContent = mapOf(
                                "categoryIcon" to InlineTextContent(
                                    Placeholder(
                                        width = 16.sp,
                                        height = 16.sp,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                    )
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(task.category),
                                        contentDescription = task.category,
                                        tint = getCategoryAccentColor(task.category, colorSchemeType, isDark),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                            Text(
                                text = annotatedText,
                                inlineContent = inlineContent,
                                fontWeight = FontWeight.Bold,
                                fontSize = AppFontSizes.large,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = AppFontSizes.large,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                val badgeStyle = if (colorSchemeType == "minimal") {
                    getMinimalPriorityColors(task.priority, isCompleted, isDark)
                } else {
                    val containerCol = getPriorityBoxColor(task.priority, isCompleted)
                    val textCol = if (task.priority in 3..4 || isCompleted) Color.DarkGray else Color.White
                    PriorityThemeBadgeColors(containerCol, textCol, Color.Transparent)
                }
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeStyle.containerColor)
                        .then(
                            if (colorSchemeType == "minimal") {
                                Modifier.border(1.dp, badgeStyle.borderColor, RoundedCornerShape(6.dp))
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = task.priority.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = AppFontSizes.micro,
                        color = badgeStyle.contentColor
                    )
                }
            }
            
            if (showDescription && task.description.isNotBlank()) {
                Text(
                    text = parseStyledDescription(task.description),
                    fontSize = AppFontSizes.medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (task.subTasks.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    task.subTasks.forEach { subTask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (subTask.isCompleted) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (subTask.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = subTask.title,
                                fontSize = AppFontSizes.small,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            if (task.reminderTime != null || task.isRecurring) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    if (task.reminderTime != null) {
                        val isReminderActive = task.isReminderActive
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isReminderActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = if (isReminderActive) "Active Reminder" else "Suspended Reminder",
                                tint = if (isReminderActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                            val statusText = DateTimeUtils.formatAlarmTime(context, task.reminderTime) +
                                    if (isReminderActive && task.repeatedTimes > 0) " (repeated ${task.repeatedTimes}x)" else if (!isReminderActive) " (suspended)" else ""
                            Text(
                                text = statusText,
                                fontSize = AppFontSizes.extraSmall,
                                color = if (isReminderActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    if (task.isRecurring) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Recurring Task",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Everyday",
                                fontSize = AppFontSizes.extraSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private val previewTasks = listOf(
    Task(
        id = 1,
        title = "Launch Marketing Campaign",
        description = "Coordinate with design and content team on assets:\n- Finalize ad copy\n- Review banner dimensions\n- Schedule social posts",
        priority = 1,
        dateAdded = "2026-08-15",
        isCompleted = false,
        category = "Work",
        reminderTime = System.currentTimeMillis() + 7200000,
        repeatedTimes = 2,
        isRecurring = true,
        subTasks = listOf(
            SubTask("Finalize ad copy", true),
            SubTask("Review banner dimensions", false),
            SubTask("Schedule social posts", false)
        )
    ),
    Task(
        id = 2,
        title = "Team Standup Meeting",
        description = "Discuss sprint deliverables and blockers",
        priority = 2,
        dateAdded = "2026-08-15",
        isCompleted = true,
    ),
    Task(
        id = 3,
        title = "Evening Cardio & Stretch",
        description = "5km run followed by stretching",
        priority = 3,
        dateAdded = "2026-08-15",
        isCompleted = false,
        category = "Fitness"
    ),
    Task(
        id = 4,
        title = "Review Weekly Budget",
        description = "Check pending invoices and subscriptions",
        priority = 2,
        dateAdded = "2026-08-14",
        isCompleted = true,
        category = "Finance"
    ),
    Task(
        id = 5,
        title = "Grocery Restock",
        description = "Milk, Eggs, Olive Oil, Greek Yogurt",
        priority = 4,
        dateAdded = "2026-08-14",
        isCompleted = false,
        category = "Errands"
    )
)

private val previewStickyTasks = listOf(
    Task(
        id = 10,
        title = "Daily Journaling & Reading",
        description = "Read 20 pages and write daily reflection",
        priority = 2,
        dateAdded = "2026-08-14",
        category = "Sticky",
        isRecurring = true
    )
)

@Preview(showBackground = true, name = "Expanded View - Minimal Light (Single Column)")
@Composable
fun ExpandedViewMinimalLightPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpandedView(
                tasks = previewTasks,
                stickyTasks = previewStickyTasks,
                colorSchemeType = "minimal",
                isDoubleColumn = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Expanded View - Double Column Layout")
@Composable
fun ExpandedViewDoubleColumnPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpandedView(
                tasks = previewTasks,
                stickyTasks = previewStickyTasks,
                colorSchemeType = "minimal",
                isDoubleColumn = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Expanded View - Colorful Dark Theme")
@Composable
fun ExpandedViewColorfulDarkPreview() {
    SoftTodoTheme(colorSchemeType = "colorful", themeMode = "dark") {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpandedView(
                tasks = previewTasks,
                stickyTasks = previewStickyTasks,
                colorSchemeType = "colorful",
                isDoubleColumn = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Expanded View - Simple Theme")
@Composable
fun ExpandedViewSimpleThemePreview() {
    SoftTodoTheme(colorSchemeType = "simple", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpandedView(
                tasks = previewTasks,
                stickyTasks = emptyList(),
                colorSchemeType = "simple",
                isDoubleColumn = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Expanded View - Empty State")
@Composable
fun ExpandedViewEmptyPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpandedView(
                tasks = emptyList(),
                colorSchemeType = "minimal"
            )
        }
    }
}

@Preview(showBackground = true, name = "Expanded Task Row - Detailed with Subtasks & Alert")
@Composable
fun ExpandedTaskRowDetailedPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpandedTaskRow(
                task = previewTasks[0],
                colorSchemeType = "minimal",
                showDescription = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Expanded Task Row - Completed State")
@Composable
fun ExpandedTaskRowCompletedPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpandedTaskRow(
                task = previewTasks[1],
                colorSchemeType = "minimal",
                showDescription = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Expanded Task Row - Recurring Sticky Task")
@Composable
fun ExpandedTaskRowRecurringPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            ExpandedTaskRow(
                task = previewStickyTasks[0],
                colorSchemeType = "minimal",
                showDescription = true
            )
        }
    }
}
