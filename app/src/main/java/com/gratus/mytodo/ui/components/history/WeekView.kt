package com.gratus.mytodo.ui.components.history

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale as LocalLocale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.components.dialogs.getCategoryIcon
//import com.gratus.mytodo.ui.components.home.getCategoryAccentColor
import com.gratus.mytodo.ui.components.home.getPriorityBoxColor
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.getCategoryAccentColor
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Level 2: Week View (Groups tasks by Week, displaying day-wise 2-column grids)
 */
@Composable
fun WeekView(
    tasks: List<Task>,
    stickyTasks: List<Task> = emptyList(),
    colorSchemeType: String,
    onQueryChange: (String) -> Unit,
    onZoomLevelSet: (Int) -> Unit,
    onNavigateToHomeDate: ((Calendar, Int?) -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f // The line is a heuristic (a "quick rule of thumb") used to detect if the current app theme is in "Dark Mode" based on the actual color of the background. Alternatively, '.background.luminance()' < 0.5f can also be used.
    val groupedByWeek = remember(tasks) {
        tasks.groupBy { task ->
            val date = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            val firstDayOfWeek = cal.firstDayOfWeek
            while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                cal.add(Calendar.DAY_OF_MONTH, -1)
            }
            DateTimeUtils.formatDbDate(cal)
        }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        groupedByWeek.forEach { (weekStartStr, weekTasks) ->
            item(key = weekStartStr) {
                val total = weekTasks.size
                val done = weekTasks.count { it.isCompleted }
                val weekDate = DateTimeUtils.parseDbDate(weekStartStr) ?: Date()
                val weekCal = Calendar.getInstance().apply { time = weekDate }
                val weekLabel = SimpleDateFormat("MMM dd, yyyy", LocalLocale.current.platformLocale).format(weekDate)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val activeStickyInWeek = remember(stickyTasks, weekDate) {
                            val weekEnd = Calendar.getInstance().apply {
                                time = weekDate
                                add(Calendar.DAY_OF_MONTH, 6)
                            }.time
                            stickyTasks.filter { task ->
                                val stickyDate = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
                                !stickyDate.after(weekEnd)
                            }.distinctBy { it.title.trim().lowercase(Locale.ROOT) }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Week of $weekLabel",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeStickyInWeek.isNotEmpty()) {
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
                                            text = "${activeStickyInWeek.size} Sticky",
                                            fontSize = AppFontSizes.extraSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$done/$total Done",
                                        fontSize = AppFontSizes.extraSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        if (activeStickyInWeek.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                activeStickyInWeek.forEach { stickyTask ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                            .clickable { onNavigateToHomeDate?.invoke(weekCal, stickyTask.id) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(getPriorityBoxColor(stickyTask.priority, stickyTask.isCompleted))
                                            )
                                            Text(
                                                text = stickyTask.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "Everyday",
                                            fontSize = AppFontSizes.nano,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        val daysInWeek = remember(weekTasks) {
                            weekTasks.groupBy { it.dateAdded }.toSortedMap()
                        }
                        val dayList = daysInWeek.keys.toList()
                        val rowsCount = (dayList.size + 1) / 2

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (rowIndex in 0 until rowsCount) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (colIndex in 0..1) {
                                        val index = rowIndex * 2 + colIndex
                                        if (index < dayList.size) {
                                            val dayStr = dayList[index]
                                            val dayTasks = daysInWeek[dayStr] ?: emptyList()
                                            val dayDate = DateTimeUtils.parseDbDate(dayStr) ?: Date()
                                            val dayCal = Calendar.getInstance().apply { time = dayDate }
                                            val dayLabelStr = SimpleDateFormat("EEE, MMM dd", LocalLocale.current.platformLocale).format(dayDate)

                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        onQueryChange(dayStr)
                                                        onZoomLevelSet(3)
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = dayLabelStr,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = AppFontSizes.extraSmall,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                    dayTasks.take(3).forEach { task ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .then(
                                                                    if (onNavigateToHomeDate != null) {
                                                                        Modifier.clickable {
                                                                            onNavigateToHomeDate(dayCal, task.id)
                                                                        }
                                                                    } else Modifier
                                                                )
                                                                .padding(vertical = 2.dp)
                                                        ) {
                                                            if (task.category != null) {
                                                                Icon(
                                                                    imageVector = getCategoryIcon(task.category),
                                                                    contentDescription = task.category,
                                                                    tint = getCategoryAccentColor(task.category, colorSchemeType, isDark),
                                                                    modifier = Modifier.size(10.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = task.title,
                                                                fontSize = AppFontSizes.micro,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    if (dayTasks.size > 3) {
                                                        Text(
                                                            text = "+${dayTasks.size - 3} more",
                                                            fontSize = AppFontSizes.nano,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val previewTasks = listOf(
    // Current Week
    Task(id = 1, title = "Design System Review", description = "", priority = 1, dateAdded = "2026-08-15", isCompleted = true, category = "Work"),
    Task(id = 2, title = "Team Sync Meeting", description = "", priority = 2, dateAdded = "2026-08-15", isCompleted = false, category = "Work"),
    Task(id = 3, title = "Cardio Session (5km)", description = "", priority = 2, dateAdded = "2026-08-14", isCompleted = true, category = "Fitness"),
    Task(id = 4, title = "Grocery Run", description = "", priority = 3, dateAdded = "2026-08-14", isCompleted = false, category = "Errands"),
    Task(id = 5, title = "Review Pull Requests", description = "", priority = 1, dateAdded = "2026-08-13", isCompleted = true, category = "Work"),
    Task(id = 6, title = "Pay Electricity Bill", description = "", priority = 2, dateAdded = "2026-08-13", isCompleted = true, category = "Finance"),
    Task(id = 7, title = "Buy Protein Powder", description = "", priority = 4, dateAdded = "2026-08-13", isCompleted = false, category = "Fitness"),
    Task(id = 8, title = "Dentist Appointment", description = "", priority = 1, dateAdded = "2026-08-13", isCompleted = false, category = "Health"),
    Task(id = 9, title = "Water Plants", description = "", priority = 4, dateAdded = "2026-08-12", isCompleted = true, category = "Home"),
    Task(id = 10, title = "Kotlin Coroutines Study", description = "", priority = 3, dateAdded = "2026-08-11", isCompleted = true, category = "Learn"),
    // Previous Week
    Task(id = 11, title = "Monthly Budget Review", description = "", priority = 1, dateAdded = "2026-08-08", isCompleted = true, category = "Finance"),
    Task(id = 12, title = "Sprint Retrospective", description = "", priority = 2, dateAdded = "2026-08-07", isCompleted = true, category = "Work"),
    Task(id = 13, title = "Weekend Hike", description = "", priority = 3, dateAdded = "2026-08-06", isCompleted = true, category = "Fitness")
)

private val previewStickyTasks = listOf(
    Task(
        id = 100,
        title = "Daily Morning Pushups",
        description = "50 reps before breakfast",
        priority = 2,
        dateAdded = "2026-08-10",
        category = "Sticky",
        isRecurring = true
    ),
    Task(
        id = 101,
        title = "Read 20 pages",
        description = "Non-fiction book",
        priority = 3,
        dateAdded = "2026-08-10",
        category = "Sticky",
        isRecurring = true
    )
)

@Preview(showBackground = true, name = "Week View - Minimal Light")
@Composable
fun WeekViewMinimalLightPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            WeekView(
                tasks = previewTasks,
                stickyTasks = previewStickyTasks,
                colorSchemeType = "minimal",
                onQueryChange = {},
                onZoomLevelSet = {},
                onNavigateToHomeDate = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "Week View - Colorful Dark Theme")
@Composable
fun WeekViewColorfulDarkPreview() {
    SoftTodoTheme(colorSchemeType = "colorful", themeMode = "dark") {
        Box(modifier = Modifier.padding(16.dp)) {
            WeekView(
                tasks = previewTasks,
                stickyTasks = previewStickyTasks,
                colorSchemeType = "colorful",
                onQueryChange = {},
                onZoomLevelSet = {},
                onNavigateToHomeDate = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "Week View - Simple Theme")
@Composable
fun WeekViewSimpleThemePreview() {
    SoftTodoTheme(colorSchemeType = "simple", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            WeekView(
                tasks = previewTasks,
                stickyTasks = emptyList(),
                colorSchemeType = "simple",
                onQueryChange = {},
                onZoomLevelSet = {},
                onNavigateToHomeDate = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "Week View - Overflow Day (+X more)")
@Composable
fun WeekViewOverflowDayPreview() {
    val overflowTasks = listOf(
        Task(id = 1, title = "Morning Standup", description = "", priority = 1, dateAdded = "2026-08-15", isCompleted = true, category = "Work"),
        Task(id = 2, title = "Design Review", description = "", priority = 2, dateAdded = "2026-08-15", isCompleted = true, category = "Work"),
        Task(id = 3, title = "Backend API Integration", description = "", priority = 1, dateAdded = "2026-08-15", isCompleted = false, category = "Work"),
        Task(id = 4, title = "Write Unit Tests", description = "", priority = 2, dateAdded = "2026-08-15", isCompleted = false, category = "Work"),
        Task(id = 5, title = "Deploy to Staging", description = "", priority = 3, dateAdded = "2026-08-15", isCompleted = false, category = "Work"),
        Task(id = 6, title = "Evening Gym", description = "", priority = 4, dateAdded = "2026-08-15", isCompleted = false, category = "Fitness")
    )
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            WeekView(
                tasks = overflowTasks,
                stickyTasks = emptyList(),
                colorSchemeType = "minimal",
                onQueryChange = {},
                onZoomLevelSet = {},
                onNavigateToHomeDate = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "Week View - Empty State")
@Composable
fun WeekViewEmptyPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.padding(16.dp)) {
            WeekView(
                tasks = emptyList(),
                stickyTasks = emptyList(),
                colorSchemeType = "minimal",
                onQueryChange = {},
                onZoomLevelSet = {},
                onNavigateToHomeDate = { _, _ -> }
            )
        }
    }
}
