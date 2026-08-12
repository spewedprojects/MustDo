package com.gratus.mytodo.ui.components.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Level 0: Year View (Groups tasks by Year, displaying 12-month grids)
 */
@Composable
fun YearView(
    tasks: List<Task>,
    stickyTasks: List<Task> = emptyList(),
    colorSchemeType: String,
    onQueryChange: (String) -> Unit,
    onZoomLevelSet: (Int) -> Unit
) {
    val groupedByYear = remember(tasks) {
        tasks.groupBy { task ->
            val date = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.get(Calendar.YEAR).toString()
        }.toSortedMap(compareByDescending { it })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        groupedByYear.forEach { (year, yearTasks) ->
            item(key = year) {
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
                        Text(
                            text = year,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        val monthsInYear = remember(yearTasks) {
                            yearTasks.groupBy { task ->
                                val date = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
                                val cal = Calendar.getInstance()
                                cal.time = date
                                cal.get(Calendar.MONTH)
                            }
                        }

                        val monthsAbbr = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (rowIndex in 0 until 4) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (colIndex in 0..2) {
                                        val monthIndex = rowIndex * 3 + colIndex
                                        val monthLabel = monthsAbbr[monthIndex]
                                        val monthTasks = monthsInYear[monthIndex] ?: emptyList()
                                        val total = monthTasks.size
                                        val done = monthTasks.count { it.isCompleted }
                                        val monthDbStr = String.format(Locale.US, "%s-%02d", year, monthIndex + 1)

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    onQueryChange(monthDbStr)
                                                    onZoomLevelSet(1)
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
                                                    text = monthLabel,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = AppFontSizes.medium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "$done/$total",
                                                    fontSize = AppFontSizes.extraSmall,
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
