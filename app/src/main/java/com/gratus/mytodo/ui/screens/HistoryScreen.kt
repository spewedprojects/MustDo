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

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.gratus.mytodo.ui.utils.detectPinchZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.data.Task

import com.gratus.mytodo.ui.FilterOption
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.*
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*
import com.gratus.mytodo.ui.components.getCategoryIcon
import com.gratus.mytodo.ui.screens.getCategoryAccentColor
import androidx.compose.ui.platform.LocalLocale

/**
 * Historical records screen with filtering, date classification, and structural pinch-to-zoom.
 */
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    colorSchemeType: String,
    onNavigateToHomeDate: ((Calendar) -> Unit)? = null
) {
    val tasks by viewModel.historyTasks.collectAsState(initial = emptyList())
    val query by viewModel.searchQuery.collectAsState()
    val zoomLevel by viewModel.historyZoomLevel.collectAsState()
    val activeFilter by viewModel.historyFilter.collectAsState()

    HistoryScreenContent(
        tasks = tasks,
        query = query,
        zoomLevel = zoomLevel,
        activeFilter = activeFilter,
        colorSchemeType = colorSchemeType,
        onQueryChange = { viewModel.setSearchQuery(it) },
        onZoomChange = { viewModel.zoomHistory(it) },
        onZoomLevelSet = { viewModel.setHistoryZoom(it) },
        onFilterChange = { viewModel.setHistoryFilter(it) },
        onNavigateToHomeDate = onNavigateToHomeDate
    )
}

/**
 * Stateless version of HistoryScreen for preview and testing.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    tasks: List<Task>,
    query: String,
    zoomLevel: Int,
    activeFilter: FilterOption,
    colorSchemeType: String,
    onQueryChange: (String) -> Unit,
    onZoomChange: (Int) -> Unit,
    onZoomLevelSet: (Int) -> Unit,
    onFilterChange: (FilterOption) -> Unit,
    onNavigateToHomeDate: ((Calendar) -> Unit)? = null
) {
    val context = LocalContext.current

    // State to throttling scale gestures
    var lastGestureTime by remember { mutableLongStateOf(0L) }
    var isDoubleColumnInDayView by rememberSaveable { mutableStateOf(false) }

    // Pinch to Zoom math utilizing custom cumulative pinch detector
    val pinchZoomModifier = Modifier.pointerInput(Unit) {
        detectPinchZoom(
            onZoomIn = {
                val now = System.currentTimeMillis()
                if (now - lastGestureTime >= 150) {
                    onZoomChange(1) // zoom in
                    lastGestureTime = now
                }
            },
            onZoomOut = {
                val now = System.currentTimeMillis()
                if (now - lastGestureTime >= 150) {
                    onZoomChange(-1) // zoom out
                    lastGestureTime = now
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(pinchZoomModifier)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Dedicated controls layout inside the screen for gorgeous scannability
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { onQueryChange(it) },
                    placeholder = { Text("Search title, description...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Secondary Controls Row: Date Picker, Display type, Filter Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date pick picker trigger
                    IconButton(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val picked = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    val queryStr = DateTimeUtils.formatDbDate(picked)
                                    onQueryChange(queryStr) // Filter by picked date
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date as Filter",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (zoomLevel == 3) {
                        IconButton(
                            onClick = { isDoubleColumnInDayView = !isDoubleColumnInDayView }
                        ) {
                            Icon(
                                imageVector = if (isDoubleColumnInDayView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                contentDescription = if (isDoubleColumnInDayView) "Switch to Single Column" else "Switch to Double Column",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }


                    // Filter dropdown
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        listOf(
                            Pair(FilterOption.ALL, "All"),
                            Pair(FilterOption.MARKED_COMPLETE, "Done"),
                            Pair(FilterOption.LEFT_INCOMPLETE, "Pending")
                        ).forEach { (opt, label) ->
                            val active = activeFilter == opt
                            Box(
                                modifier = Modifier
                                    .clickable { onFilterChange(opt) }
                                    .background(
                                        if (active) MaterialTheme.colorScheme.secondaryContainer 
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                MainFontText(
                                    text = label,
                                    fontSize = AppFontSizes.extraSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer 
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Timeline Content List with AnimatedContent to handle cross-fades
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history matches your search filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            AnimatedContent(
                targetState = zoomLevel,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "HistoryZoomTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_tasks_list")
            ) { targetZoom ->
                when (targetZoom) {
                    0 -> YearView(
                        tasks = tasks,
                        colorSchemeType = colorSchemeType,
                        onQueryChange = onQueryChange,
                        onZoomLevelSet = onZoomLevelSet
                    )
                    1 -> MonthView(
                        tasks = tasks,
                        colorSchemeType = colorSchemeType,
                        onZoomLevelSet = onZoomLevelSet
                    )
                    2 -> WeekView(
                        tasks = tasks,
                        colorSchemeType = colorSchemeType,
                        onQueryChange = onQueryChange,
                        onZoomLevelSet = onZoomLevelSet,
                        onNavigateToHomeDate = onNavigateToHomeDate
                    )
                    3 -> ExpandedView(
                        tasks = tasks,
                        colorSchemeType = colorSchemeType,
                        onNavigateToHomeDate = onNavigateToHomeDate,
                        isDoubleColumn = isDoubleColumnInDayView
                    )
                    else -> ExpandedView(
                        tasks = tasks,
                        colorSchemeType = colorSchemeType,
                        onNavigateToHomeDate = onNavigateToHomeDate,
                        isDoubleColumn = isDoubleColumnInDayView
                    )
                }
        }
    }
}

    // Horizontal Zoom floating action buttons placed at bottom right corner
    Row(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingActionButton(
            onClick = { onZoomChange(-1) },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                modifier = Modifier.size(20.dp)
            )
        }
        
        FloatingActionButton(
            onClick = { onZoomChange(1) },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Zoom In",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
}

/**
 * Composable text that uses App Font Sizes.
 */
@Composable
fun MainFontText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

/**
 * Level 0: Year View (Groups tasks by Year, displaying 12-month grids)
 */
@Composable
fun YearView(
    tasks: List<Task>,
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

/**
 * Level 1: Month View (Groups tasks by Month, displaying weeks within the month)
 */
@Composable
fun MonthView(
    tasks: List<Task>,
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
                        Text(
                            text = monthStr,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

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

/**
 * Level 2: Week View (Groups tasks by Week, displaying day-wise 2-column grids)
 */
@Composable
fun WeekView(
    tasks: List<Task>,
    colorSchemeType: String,
    onQueryChange: (String) -> Unit,
    onZoomLevelSet: (Int) -> Unit,
    onNavigateToHomeDate: ((Calendar) -> Unit)? = null
) {
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
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$done/$total Done",
                                    fontSize = AppFontSizes.extraSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

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
                                                        val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.then(
                                                                if (onNavigateToHomeDate != null) {
                                                                    Modifier.clickable {
                                                                        val dateObj = DateTimeUtils.parseDbDate(task.dateAdded) ?: Date()
                                                                        val cal = Calendar.getInstance().apply { time = dateObj }
                                                                        onNavigateToHomeDate(cal)
                                                                    }
                                                                } else Modifier
                                                            )
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

/**
 * Level 3: Expanded View (Groups tasks by exact date, shows continuous vertical details stream)
 */
@Composable
fun ExpandedView(
    tasks: List<Task>,
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
                        Text(
                            text = DateTimeUtils.formatHistoryGroup(dateObj),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        
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
                                tint = getCategoryAccentColor(task.category, colorSchemeType, isDark),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = task.category.uppercase(),
                                fontSize = AppFontSizes.nano,
                                fontWeight = FontWeight.Bold,
                                color = getCategoryAccentColor(task.category, colorSchemeType, isDark)
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

@Preview(showBackground = true, name = "History Screen - Simple Light")
@Composable
fun HistoryScreenPreview() {
    SoftTodoTheme(colorSchemeType = "simple") {
        HistoryScreenContent(
            tasks = sampleHistoryTasks,
            query = "",
            zoomLevel = 3,
            activeFilter = FilterOption.ALL,
            colorSchemeType = "simple",
            onQueryChange = {},
            onZoomChange = {},
            onZoomLevelSet = {},
            onFilterChange = {}
        )
    }
}

@Preview(showBackground = true, name = "History Screen - Colorful Dark")
@Composable
fun HistoryScreenColorfulDarkPreview() {
    SoftTodoTheme(colorSchemeType = "colorful", themeMode = "dark") {
        HistoryScreenContent(
            tasks = sampleHistoryTasks,
            query = "",
            zoomLevel = 3,
            activeFilter = FilterOption.ALL,
            colorSchemeType = "colorful",
            onQueryChange = {},
            onZoomChange = {},
            onZoomLevelSet = {},
            onFilterChange = {}
        )
    }
}

@Preview(showBackground = true, name = "History Screen - Empty State")
@Composable
fun HistoryScreenEmptyPreview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
        HistoryScreenContent(
            tasks = emptyList(),
            query = "",
            zoomLevel = 3,
            activeFilter = FilterOption.ALL,
            colorSchemeType = "minimal",
            onQueryChange = {},
            onZoomChange = {},
            onZoomLevelSet = {},
            onFilterChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Zoomable Task Row - Level 1 (Compact)")
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

@Preview(showBackground = true, name = "Zoomable Task Row - Level 2 (Medium)")
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

@Preview(showBackground = true, name = "Zoomable Task Row - Level 3 (Detailed)")
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

private val sampleHistoryTasks = listOf(
    Task(id = 1, title = "Finish Project Proposal", description = "Finalize the budget and timeline", priority = 1, dateAdded = "2023-10-25", isCompleted = true),
    Task(id = 2, title = "Grocery Shopping", description = "Milk, Eggs, Bread, Fruits", priority = 2, dateAdded = "2023-10-25", isCompleted = true),
    Task(id = 3, title = "Gym Workout", description = "Leg day", priority = 3, dateAdded = "2023-10-24", isCompleted = false)
)
