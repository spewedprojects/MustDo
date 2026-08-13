package com.gratus.mytodo.ui.components.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.intl.Locale as LocalLocale
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Level 1: Month View (Groups tasks by Month, displaying weeks within the month)
 */
@Composable
fun MonthView(
    tasks: List<Task>,
    stickyTasks: List<Task> = emptyList(),
    colorSchemeType: String,
    onZoomLevelSet: (Int) -> Unit
) {
    val groupedByMonth = remember(tasks) {
        tasks.groupBy { task ->
            val date = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            DateTimeUtils.formatMonthYear(cal.time)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        groupedByMonth.forEach { (monthStr, monthTasks) ->
            item(key = monthStr) {
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
                        val activeStickyInMonth = remember(stickyTasks, monthStr) {
                            stickyTasks.distinctBy { it.title.trim().lowercase(Locale.ROOT) }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthStr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (activeStickyInMonth.isNotEmpty()) {
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
                                        text = "${activeStickyInMonth.size} Sticky",
                                        fontSize = AppFontSizes.extraSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        if (activeStickyInMonth.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                activeStickyInMonth.forEach { stickyTask ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
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

                        val weeksInMonth = remember(monthTasks) {
                            monthTasks.groupBy { task ->
                                val date = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
                                val cal = Calendar.getInstance()
                                cal.time = date
                                val firstDayOfWeek = cal.firstDayOfWeek
                                while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
                                    cal.add(Calendar.DAY_OF_MONTH, -1)
                                }
                                DateTimeUtils.formatDbDate(cal)
                            }.toSortedMap()
                        }

                        val weekList = weeksInMonth.keys.toList()
                        val rowsCount = (weekList.size + 1) / 2

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
                                        if (index < weekList.size) {
                                            val weekStartStr = weekList[index]
                                            val weekTasks = weeksInMonth[weekStartStr] ?: emptyList()
                                            val total = weekTasks.size
                                            val done = weekTasks.count { it.isCompleted }
                                            val weekDate = DateTimeUtils.parseDbDate(weekStartStr) ?: Date()
                                            val weekLabel = SimpleDateFormat("MMM dd", LocalLocale.current.platformLocale).format(weekDate)

                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        onZoomLevelSet(2)
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(10.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = "Week of $weekLabel",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = AppFontSizes.extraSmall,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "$done/$total tasks",
                                                        fontSize = AppFontSizes.micro,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    LinearProgressIndicator(
                                                        progress = { if (total > 0) done.toFloat() / total.toFloat() else 0f },
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.6f)
                                                            .height(3.dp)
                                                            .clip(RoundedCornerShape(50)),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                                    )
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "History Month View")
@Composable
fun MonthViewPreview() {
    val sampleTasks = listOf(
        Task(id = 1, title = "Task 1", description = "", priority = 1, dateAdded = "2026-08-13", isCompleted = true),
        Task(id = 2, title = "Task 2", description = "", priority = 2, dateAdded = "2026-08-14")
    )
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        MonthView(
            tasks = sampleTasks,
            colorSchemeType = "minimal",
            onZoomLevelSet = {}
        )
    }
}
