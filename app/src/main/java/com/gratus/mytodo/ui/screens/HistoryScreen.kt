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

import com.gratus.mytodo.ui.components.history.ExpandedView
import com.gratus.mytodo.ui.components.history.MainFontText
import com.gratus.mytodo.ui.components.history.MonthView
import com.gratus.mytodo.ui.components.history.WeekView
import com.gratus.mytodo.ui.components.history.YearView
import com.gratus.mytodo.ui.components.history.ZoomableTaskRow

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
import com.gratus.mytodo.ui.components.dialogs.getCategoryIcon
import com.gratus.mytodo.ui.components.home.getCategoryAccentColor
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

    val isStickyEnabled by viewModel.isStickyEnabled.collectAsState()

    HistoryScreenContent(
        tasks = tasks,
        query = query,
        zoomLevel = zoomLevel,
        activeFilter = activeFilter,
        colorSchemeType = colorSchemeType,
        isStickyEnabled = isStickyEnabled,
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
    isStickyEnabled: Boolean = true,
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

        val stickyTasks = remember(tasks, isStickyEnabled) {
            if (!isStickyEnabled) emptyList()
            else tasks.filter { it.category?.equals("Sticky", ignoreCase = true) == true }
        }
        val regularTasks = remember(tasks) {
            tasks.filter { it.category?.equals("Sticky", ignoreCase = true) != true }
        }

        // Timeline Content List with AnimatedContent to handle cross-fades
        if (regularTasks.isEmpty() && stickyTasks.isEmpty()) {
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
                        tasks = regularTasks,
                        stickyTasks = stickyTasks,
                        colorSchemeType = colorSchemeType,
                        onQueryChange = onQueryChange,
                        onZoomLevelSet = onZoomLevelSet
                    )
                    1 -> MonthView(
                        tasks = regularTasks,
                        stickyTasks = stickyTasks,
                        colorSchemeType = colorSchemeType,
                        onZoomLevelSet = onZoomLevelSet
                    )
                    2 -> WeekView(
                        tasks = regularTasks,
                        stickyTasks = stickyTasks,
                        colorSchemeType = colorSchemeType,
                        onQueryChange = onQueryChange,
                        onZoomLevelSet = onZoomLevelSet,
                        onNavigateToHomeDate = onNavigateToHomeDate
                    )
                    3 -> ExpandedView(
                        tasks = regularTasks,
                        stickyTasks = stickyTasks,
                        colorSchemeType = colorSchemeType,
                        onNavigateToHomeDate = onNavigateToHomeDate,
                        isDoubleColumn = isDoubleColumnInDayView
                    )
                    else -> ExpandedView(
                        tasks = regularTasks,
                        stickyTasks = stickyTasks,
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
