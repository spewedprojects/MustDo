package com.gratus.mytodo.ui.components.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.ui.StatsData

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

                    val maxTotal = stats.weeklyHistory.maxOfOrNull { it.total } ?: 5
                    val maxScale = maxOf(maxTotal, 5)

                    val barGroupWidth = chartWidth / 7f
                    val individualBarWidth = barGroupWidth * 0.3f

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

                    stats.weeklyHistory.forEachIndexed { index, daily ->
                        val xPos = paddingLeft + (index * barGroupWidth)

                        val totalBarHeight = if (daily.total > 0) {
                            (daily.total.toFloat() / maxScale) * chartHeight
                        } else {
                            4.dp.toPx()
                        }

                        val completedBarHeight = if (daily.completed > 0) {
                            (daily.completed.toFloat() / maxScale) * chartHeight
                        } else {
                            0f
                        }

                        drawRoundRect(
                            color = primaryContainerColor,
                            topLeft = Offset(xPos + individualBarWidth / 2, chartHeight - totalBarHeight),
                            size = Size(individualBarWidth, totalBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

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
