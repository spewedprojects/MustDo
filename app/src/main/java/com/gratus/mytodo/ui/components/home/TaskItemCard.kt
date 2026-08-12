package com.gratus.mytodo.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.SubTask
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.MinimalDarkCardBorder
import com.gratus.mytodo.ui.theme.MinimalLightCardBorder
import com.gratus.mytodo.ui.theme.PriorityThemeBadgeColors
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.getMinimalPriorityColors
import com.gratus.mytodo.ui.utils.DateTimeUtils

/**
 * Task item card component.
 */
@Composable
fun TaskItemCard(
    task: Task,
    colorSchemeType: String,
    isFlat: Boolean = false,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSubComplete: (Int) -> Unit
) {
    val isCompleted = task.isCompleted
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val context = LocalContext.current

    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mark Completed icon button
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                    .align(Alignment.Top)
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark done status",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Mark done status",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title & Description (Markdown parsed dynamically)
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parseStyledDescription(task.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCompleted) 0.5f else 0.8f)
                    )
                }

                if (task.subTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        task.subTasks.forEachIndexed { index, subTask ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleSubComplete(index) }
                                    .padding(vertical = 2.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (subTask.isCompleted) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = "Toggle Subtask",
                                    tint = if (subTask.isCompleted || isCompleted) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = subTask.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = if (subTask.isCompleted || isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (subTask.isCompleted || isCompleted) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    }
                                )
                            }
                        }
                    }
                }

                // Show Scheduled Alarm timestamp indicator
                if (task.reminderTime != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val isReminderActive = task.isReminderActive
                    val snoozeUntil = task.snoozedUntil ?: 0L
                    val isSnoozed = snoozeUntil > System.currentTimeMillis()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isSnoozed) {
                                Icons.Default.Snooze
                            } else if (isReminderActive) {
                                Icons.Default.Notifications
                            } else {
                                Icons.Default.NotificationsOff
                            },
                            contentDescription = if (isSnoozed) {
                                "Snoozed reminder"
                            } else if (isReminderActive) {
                                "Active reminder"
                            } else {
                                "Suspended reminder"
                            },
                            tint = if (isCompleted || (!isReminderActive && !isSnoozed)) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        val statusText = if (isSnoozed) {
                            "Snoozed until: " + DateTimeUtils.formatAlarmTime(context, snoozeUntil)
                        } else if (isReminderActive) {
                            "Alert scheduled: " + DateTimeUtils.formatAlarmTime(context, task.reminderTime) +
                                    if (task.repeatedTimes > 0) " (repeated ${task.repeatedTimes}x)" else ""
                        } else {
                            "Alert suspended: " + DateTimeUtils.formatAlarmTime(context, task.reminderTime)
                        }
                        Text(
                            text = statusText,
                            fontSize = AppFontSizes.micro,
                            color = if (isCompleted || (!isReminderActive && !isSnoozed)) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Priority level displayed inside a boxed container colored based on active colorSchemeType
            val badgeStyle = if (colorSchemeType == "minimal") {
                getMinimalPriorityColors(task.priority, isCompleted, isDark)
            } else {
                val containerCol = getPriorityBoxColor(task.priority, isCompleted)
                val textCol = if (task.priority == 4 || isCompleted) Color.DarkGray else Color.White
                val borderCol = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                PriorityThemeBadgeColors(containerCol, textCol, borderCol)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeStyle.containerColor)
                    .border(
                        1.dp,
                        badgeStyle.borderColor,
                        RoundedCornerShape(8.dp)
                    )
                    .align(Alignment.Top),
                contentAlignment = Alignment.Center
            ) {
                 Text(
                     text = task.priority.toString(),
                     fontWeight = FontWeight.Bold,
                     fontSize = AppFontSizes.large,
                     color = badgeStyle.contentColor
                 )
            }

            // Delete item button inside row
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (isFlat) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isCompleted) 0.8f else 1.0f)
                .pointerInput(onLongClick) {
                    detectTapGestures(
                        onLongPress = { _ -> onLongClick() }
                    )
                }
        ) {
            content()
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isCompleted) 0.8f else 1.0f)
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(onLongClick) {
                    detectTapGestures(
                        onLongPress = { _ -> onLongClick() }
                    )
                }
                .testTag("task_item_${task.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isCompleted) {
                    if (colorSchemeType == "minimal") {
                        if (isDark) Color(0x15FFFFFF) else Color(0x33B0AAB9)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = when (colorSchemeType) {
                "simple" -> borderStrokeSimple(isCompleted)
                "minimal" -> {
                    if (isCompleted) {
                        androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder)
                    }
                }
                else -> null
            },
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            content()
        }
    }
}

private val sampleTasks = listOf(
    Task(
        id = 1,
        title = "Finish Project Proposal",
        description = "Finalize the budget and timeline",
        priority = 1,
        dateAdded = "2023-10-27",
        category = "Work",
        subTasks = listOf(SubTask("Draft budget", true), SubTask("Set timeline", false))
    ),
    Task(
        id = 2,
        title = "Grocery Shopping",
        description = "Milk, Eggs, Bread, Fruits",
        priority = 2,
        dateAdded = "2023-10-27",
        isCompleted = true,
        category = "Errands"
    ),
    Task(
        id = 3,
        title = "Gym Workout",
        description = "Leg day",
        priority = 3,
        dateAdded = "2023-10-27",
        category = "Fitness"
    ),
    Task(
        id = 4,
        title = "Walk the dog",
        description = "Around the park",
        priority = 4,
        dateAdded = "2023-10-27"
    )
)

@Preview(showBackground = true, name = "Task Item Card")
@Composable
fun TaskItemCardPreview() {
    SoftTodoTheme(colorSchemeType = "colorful") {
        TaskItemCard(
            task = sampleTasks[0],
            colorSchemeType = "colorful",
            onToggleComplete = {},
            onDelete = {},
            onLongClick = {},
            onToggleSubComplete = {}
        )
    }
}

@Preview(showBackground = true, name = "Task Item Card - Completed")
@Composable
fun TaskItemCardCompletedPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            TaskItemCard(
                task = sampleTasks[1],
                colorSchemeType = "minimal",
                onToggleComplete = {},
                onDelete = {},
                onLongClick = {},
                onToggleSubComplete = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Task Item Card - Low Priority")
@Composable
fun TaskItemCardLowPriorityPreview() {
    SoftTodoTheme(colorSchemeType = "simple", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            TaskItemCard(
                task = sampleTasks[3],
                colorSchemeType = "simple",
                onToggleComplete = {},
                onDelete = {},
                onLongClick = {},
                onToggleSubComplete = {}
            )
        }
    }
}
