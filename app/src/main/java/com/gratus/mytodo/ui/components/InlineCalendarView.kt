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

package com.gratus.mytodo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom inline expandable Compose Calendar View.
 * Displays a 7-column month grid with task indicator dots, today highlight,
 * and date selection capabilities.
 */
@Composable
fun InlineCalendarView(
    modifier: Modifier = Modifier,
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit,
    taskDates: Set<String> = emptySet(),
    colorSchemeType: String = "minimal"
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val scope = rememberCoroutineScope()

    // Use a large number of pages to simulate infinite swiping
    val initialPage = 5000
    val pagerState = rememberPagerState(initialPage = initialPage) { 10000 }

    // Derive the displayed month from the pager state
    val currentMonthCal = remember(pagerState.currentPage, selectedDate) {
        val cal = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.add(Calendar.MONTH, pagerState.currentPage - initialPage)
        cal
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val todayCal = remember { Calendar.getInstance() }
    val todayStr = remember { DateTimeUtils.formatDbDate(todayCal) }
    val selectedStr = remember(selectedDate) { DateTimeUtils.formatDbDate(selectedDate) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Card( // removed padding to leave it up to the caller
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colorSchemeType == "minimal") {
                if (isDark) Color(0x22FFFFFF) else Color(0x66F1F5F9)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        border = if (colorSchemeType == "minimal" || colorSchemeType == "simple") {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0x22FFFFFF) else Color(0x33E2E8F0)
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Month Title & Month Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthYearFormat.format(currentMonthCal.time),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Days of Week Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = AppFontSizes.extraSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Days Grid with HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) { page ->
                val monthCal = remember(page, selectedDate) {
                    val cal = Calendar.getInstance().apply {
                        time = selectedDate.time
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    cal.add(Calendar.MONTH, page - initialPage)
                    cal
                }
                
                MonthGrid(
                    monthCal = monthCal,
                    todayStr = todayStr,
                    selectedStr = selectedStr,
                    taskDates = taskDates,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    monthCal: Calendar,
    todayStr: String,
    selectedStr: String,
    taskDates: Set<String>,
    onDateSelected: (Calendar) -> Unit
) {
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Adjust day of week index so Monday = 0, Sunday = 6
    val firstDayOfWeek = (monthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val totalGridSlots = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var currentSlot = 0
        while (currentSlot < totalGridSlots) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (i in 0 until 7) {
                    val dayNumber = currentSlot - firstDayOfWeek + 1
                    val isValidDay = dayNumber in 1..daysInMonth

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isValidDay) {
                            val cellCal = (monthCal.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, dayNumber)
                            }
                            val dateStr = DateTimeUtils.formatDbDate(cellCal)
                            val isToday = dateStr == todayStr
                            val isSelected = dateStr == selectedStr
                            val hasTask = taskDates.contains(dateStr)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable {
                                        onDateSelected(cellCal)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontSize = AppFontSizes.medium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (hasTask) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    currentSlot++
                }
            }
        }
    }
}

@Preview(showBackground = false, name = "Inline Calendar View")
@Composable
fun InlineCalendarViewPreview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
        InlineCalendarView(
            selectedDate = Calendar.getInstance(),
            onDateSelected = {},
            taskDates = setOf("2026-08-24", "2026-08-18", "2026-08-20"),
            colorSchemeType = "minimal"
        )
    }
}