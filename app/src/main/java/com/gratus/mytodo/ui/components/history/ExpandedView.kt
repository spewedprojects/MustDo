package com.gratus.mytodo.ui.components.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.components.dialogs.getCategoryIcon
import com.gratus.mytodo.ui.components.home.getCategoryAccentColor
import com.gratus.mytodo.ui.components.home.getPriorityBoxColor
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.MinimalDarkCardBorder
import com.gratus.mytodo.ui.theme.MinimalLightCardBorder
import com.gratus.mytodo.ui.theme.PriorityThemeBadgeColors
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
    onNavigateToHomeDate: ((Calendar) -> Unit)? = null,
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
            val groupTasks = grouped[dateStr] ?: emptyList()
            
            item(key = dateStr) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = when (colorSchemeType) {
                        "simple" -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        "minimal" -> {
                            val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                            androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
                        }
                        else -> null
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeStickyForDay = remember(stickyTasks, dateObj) {
                            stickyTasks.filter { task ->
                                val stickyDate = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
                                !stickyDate.after(dateObj)
                            }.distinctBy { it.title.trim().lowercase(Locale.ROOT) }
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

                            if (activeStickyForDay.isNotEmpty()) {
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
                                        text = "${activeStickyForDay.size} Sticky",
                                        fontSize = AppFontSizes.extraSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        
                        if (activeStickyForDay.isNotEmpty()) {
                            activeStickyForDay.forEach { stickyTask ->
                                ExpandedTaskRow(
                                    task = stickyTask,
                                    colorSchemeType = colorSchemeType,
                                    onNavigateToHomeDate = onNavigateToHomeDate,
                                    showDescription = !isDoubleColumn
                                )
                            }
                        }
                        
                        if (isDoubleColumn) {
                            val col1Tasks = groupTasks.filterIndexed { index, _ -> index % 2 == 0 }
                            val col2Tasks = groupTasks.filterIndexed { index, _ -> index % 2 == 1 }

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
                                                colorSchemeType = colorSchemeType,
                                                onNavigateToHomeDate = onNavigateToHomeDate,
                                                showDescription = false
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            groupTasks.forEach { task ->
                                ExpandedTaskRow(
                                    task = task,
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
    colorSchemeType: String,
    onNavigateToHomeDate: ((Calendar) -> Unit)? = null,
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
                        val dateObj = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
                        val cal = Calendar.getInstance().apply { time = dateObj }
                        onNavigateToHomeDate(cal)
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
                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder)
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
                            Icon(
                                imageVector = getCategoryIcon(task.category),
                                contentDescription = task.category,
                                tint = getCategoryAccentColor(task.category),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = task.category.uppercase(),
                                fontSize = AppFontSizes.nano,
                                fontWeight = FontWeight.Bold,
                                color = getCategoryAccentColor(task.category)
                            )
                        }
                    }
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = AppFontSizes.large,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                val badgeStyle = if (colorSchemeType == "minimal") {
                    getMinimalPriorityColors(task.priority, isCompleted, isDark)
                } else {
                    val containerCol = getPriorityBoxColor(task.priority, isCompleted)
                    val textCol = if (task.priority == 4 || isCompleted) Color.DarkGray else Color.White
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "History Expanded View")
@Composable
fun ExpandedViewPreview() {
    val sampleTasks = listOf(
        Task(id = 1, title = "Proposal Review", description = "Finalize details", priority = 1, dateAdded = "2026-08-13", isCompleted = true),
        Task(id = 2, title = "Gym Workout", description = "Cardio session", priority = 2, dateAdded = "2026-08-13")
    )
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        ExpandedView(
            tasks = sampleTasks,
            colorSchemeType = "minimal"
        )
    }
}
