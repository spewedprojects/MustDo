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

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.gratus.mytodo.ui.screens

import com.gratus.mytodo.ui.components.home.CategoryCard
import com.gratus.mytodo.ui.components.home.EmptyTasksState
import com.gratus.mytodo.ui.components.home.HomeFABGroup
import com.gratus.mytodo.ui.components.home.HomeTasksPager
import com.gratus.mytodo.ui.components.home.PermissionWarningCard
import com.gratus.mytodo.ui.components.home.TaskDeleteDialog
import com.gratus.mytodo.ui.components.home.TaskItemCard
import com.gratus.mytodo.ui.components.home.borderStrokeSimple
//import com.gratus.mytodo.ui.components.home.getCategoryAccentColor
import com.gratus.mytodo.ui.components.home.getPriorityBoxColor

import android.app.DatePickerDialog
import android.content.Intent
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.CopiedTask
import com.gratus.mytodo.data.SubTask
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.SortOption
import com.gratus.mytodo.ui.components.TaskAddDialog
import com.gratus.mytodo.ui.components.FaintBackground
import com.gratus.mytodo.ui.components.InlineCalendarView
import com.gratus.mytodo.ui.components.dialogs.getCategoryIcon
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.*
import com.gratus.mytodo.ui.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.util.*

sealed interface HomeListItem {
    data class CategoryGroup(val category: String, val tasks: List<Task>) : HomeListItem
    data class TaglessTask(val task: Task) : HomeListItem
}

/**
 * HomeScreen displays the current date's tasks, supporting the date swipe gesture.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    colorSchemeType: String,
    isInlineCalendarExpanded: Boolean = false,
    onToggleInlineCalendar: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentDate by viewModel.currentDate.collectAsState()
    val lastUsedPriority by viewModel.lastUsedPriority.collectAsState()
    val sortOption by viewModel.sortingOption.collectAsState()
    val taskDates by viewModel.taskDates.collectAsState()

    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val taskToEdit by viewModel.taskToEdit.collectAsState()
    val taskToDelete by viewModel.taskToDelete.collectAsState()
    val isAlarmGranted by viewModel.isAlarmPermissionGranted.collectAsState()
    val isNotificationGranted by viewModel.isNotificationPermissionGranted.collectAsState()
    val isFullScreenGranted by viewModel.isFullScreenPermissionGranted.collectAsState()
    val copiedTask by viewModel.copiedTask.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()

    val isStickyEnabled by viewModel.isStickyEnabled.collectAsState()
    val highlightedTaskId by viewModel.highlightedTaskId.collectAsState()

    HomeScreenContent(
        currentDate = currentDate,
        lastUsedPriority = lastUsedPriority,
        colorSchemeType = colorSchemeType,
        showAddDialog = showAddDialog,
        taskToEdit = taskToEdit,
        taskToDelete = taskToDelete,
        isAlarmPermissionGranted = isAlarmGranted,
        isNotificationPermissionGranted = isNotificationGranted,
        isFullScreenPermissionGranted = isFullScreenGranted,
        copiedTask = copiedTask,
        categories = categories,
        customCategories = customCategories,
        sortOption = sortOption,
        isStickyEnabled = isStickyEnabled,
        highlightedTaskId = highlightedTaskId,
        onShowAddDialogChange = { viewModel.setShowAddDialog(it) },
        onTaskToEditChange = { viewModel.setTaskToEdit(it) },
        onTaskToDeleteChange = { viewModel.setTaskToDelete(it) },
        onNavigateDate = { viewModel.navigateDate(it) },
        onSetDate = { viewModel.setDate(it) },
        onToggleComplete = { viewModel.toggleCompleted(it) },
        onDeleteTask = { viewModel.deleteTask(it) },
        onAddTask = { t, d, p, targetDate, replicateDates, everydayCount, reminderTimeMillis, repeatCount, subTasks, category, reminderType ->
            viewModel.addTask(t, d, p, targetDate, replicateDates, everydayCount, reminderTimeMillis, repeatCount, subTasks, category, reminderType)
        },
        onEditTask = { task, t, d, p, targetDate, reminderTimeMillis, repeatCount, subTasks, category, reminderType, replicateDates, everydayCount ->
            viewModel.updateTaskFields(task, t, d, p, targetDate, reminderTimeMillis, repeatCount, subTasks, category, reminderType, replicateDates, everydayCount)
        },
        onCopy = { viewModel.setCopiedTask(it) },
        onAddCustomCategory = { viewModel.addCustomCategory(it) },
        onDeleteCustomCategory = { viewModel.deleteCustomCategory(it) },
        onToggleSubComplete = { task, index -> viewModel.toggleSubTaskCompleted(task, index) },
        getTasksForDate = { dateStr -> viewModel.getTasksForDateFlow(dateStr) },
        isInlineCalendarExpanded = isInlineCalendarExpanded,
        onToggleInlineCalendar = onToggleInlineCalendar,
        taskDates = taskDates,
        onTerminateForever = { viewModel.terminateStickyTaskForever(it) }
    )
}

/**
 * Stateless version of HomeScreen for preview and testing.
 */
@Composable
fun HomeScreenContent(
    currentDate: Calendar,
    lastUsedPriority: Int,
    colorSchemeType: String,
    showAddDialog: Boolean,
    taskToEdit: Task?,
    taskToDelete: Task?,
    isAlarmPermissionGranted: Boolean,
    isNotificationPermissionGranted: Boolean,
    isFullScreenPermissionGranted: Boolean = true,
    copiedTask: CopiedTask?,
    categories: List<String> = emptyList(),
    customCategories: List<String>,
    sortOption: SortOption = SortOption.PRIORITY,
    isStickyEnabled: Boolean = true,
    highlightedTaskId: Int? = null,
    onShowAddDialogChange: (Boolean) -> Unit = {},
    onTaskToEditChange: (Task?) -> Unit,
    onTaskToDeleteChange: (Task?) -> Unit,
    onNavigateDate: (Int) -> Unit,
    onSetDate: (Calendar) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTask: (String, String, Int, Calendar, List<String>, Int, Long?, Int, List<SubTask>, String?, String) -> Unit,
    onEditTask: (Task, String, String, Int, Calendar, Long?, Int, List<SubTask>, String?, String, List<String>, Int) -> Unit,
    onCopy: (CopiedTask) -> Unit,
    onAddCustomCategory: (String) -> Unit,
    onDeleteCustomCategory: (String) -> Unit,
    onToggleSubComplete: (Task, Int) -> Unit,
    getTasksForDate: (String) -> Flow<List<Task>>,
    isInlineCalendarExpanded: Boolean = false,
    onToggleInlineCalendar: () -> Unit = {},
    taskDates: Set<String> = emptySet(),
    onTerminateForever: ((Task) -> Unit)? = null
) {
    val context = LocalContext.current
    val preselectedCategory = remember { mutableStateOf<String?>(null) }

    // Smooth Sliding Pager setup initialized at current date's offset
    val baseDate = remember { Calendar.getInstance() }
    val initialPage = 10000
    val initialPageOffset = remember {
        DateTimeUtils.daysBetween(baseDate, currentDate)
    }
    val pagerState = rememberPagerState(initialPage = initialPage + initialPageOffset, pageCount = { 20000 })

    val daysDiff = remember(currentDate) {
        DateTimeUtils.daysBetween(baseDate, currentDate)
    }
    val targetPage = initialPage + daysDiff

    // Scroll to page when currentDate changes externally (arrows, picker, history navigation)
    LaunchedEffect(targetPage) {
        if (pagerState.currentPage != targetPage) {
            if (kotlin.math.abs(pagerState.currentPage - targetPage) > 1) {
                pagerState.scrollToPage(targetPage)
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    // Sync focus date when page changes via swipe gesture (guarded against in-progress scrolls)
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val diff = pagerState.currentPage - initialPage
            val targetCal = (baseDate.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, diff)
            }
            if (!DateTimeUtils.isSameDay(targetCal, currentDate)) {
                onSetDate(targetCal)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isAlarmPermissionGranted || !isNotificationPermissionGranted || !isFullScreenPermissionGranted) {
                PermissionWarningCard(
                    isAlarmPermissionGranted = isAlarmPermissionGranted,
                    isNotificationPermissionGranted = isNotificationPermissionGranted,
                    isFullScreenPermissionGranted = isFullScreenPermissionGranted,
                    colorSchemeType = colorSchemeType,
                    context = context
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                visible = isInlineCalendarExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                InlineCalendarView(
                    selectedDate = currentDate,
                    onDateSelected = { cal ->
                        onSetDate(cal)
                        onToggleInlineCalendar()
                    },
                    taskDates = taskDates,
                    colorSchemeType = colorSchemeType
                )
            }

            // Smooth sliding horizontal pager
            HomeTasksPager(
                pagerState = pagerState,
                initialPage = initialPage,
                baseDate = baseDate,
                colorSchemeType = colorSchemeType,
                sortOption = sortOption,
                isStickyEnabled = isStickyEnabled,
                highlightedTaskId = highlightedTaskId,
                onShowAddDialogChange = onShowAddDialogChange,
                onTaskToEditChange = onTaskToEditChange,
                onTaskToDeleteChange = onTaskToDeleteChange,
                onToggleComplete = onToggleComplete,
                onToggleSubComplete = onToggleSubComplete,
                getTasksForDate = getTasksForDate,
                onPreselectCategory = { cat -> preselectedCategory.value = cat },
                modifier = Modifier.weight(1f)
            )
        }

        // Floating Action Buttons (Add Task and optional Paste Task)
        HomeFABGroup(
            copiedTask = copiedTask,
            currentDate = currentDate,
            context = context,
            onShowAddDialogChange = onShowAddDialogChange,
            onAddTask = onAddTask,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }

    // Task Adding Overlay Dialog box configuration
    if (showAddDialog) {
        TaskAddDialog(
            initialDate = currentDate,
            lastUsedPriority = lastUsedPriority,
            colorSchemeType = colorSchemeType,
            onDismiss = {
                onShowAddDialogChange(false)
                preselectedCategory.value = null
            },
            onAddTask = { t, d, p, targetDate, replicateDates, everydayCount, reminderTimeMillis, repeatCount, subTasks, category, reminderType ->
                onAddTask(t, d, p, targetDate, replicateDates, everydayCount, reminderTimeMillis, repeatCount, subTasks, category, reminderType)
                onShowAddDialogChange(false)
                preselectedCategory.value = null
                Toast.makeText(context, "Task created!", Toast.LENGTH_SHORT).show()
            },
            onEditTask = onEditTask,
            onTerminateForever = onTerminateForever,
            preselectedCategory = preselectedCategory.value,
            copiedTask = copiedTask,
            onCopy = onCopy,
            categories = categories,
            customCategories = customCategories,
            onAddCategory = onAddCustomCategory,
            onDeleteCategory = onDeleteCustomCategory
        )
    }

    // Task Editing Dialog Box
    if (taskToEdit != null) {
        TaskAddDialog(
            initialDate = Calendar.getInstance().apply {
                time = DateTimeUtils.parseDbDate(taskToEdit.dateAdded) ?: Date()
            },
            lastUsedPriority = lastUsedPriority,
            colorSchemeType = colorSchemeType,
            taskToEdit = taskToEdit,
            onDismiss = { onTaskToEditChange(null) },
            onAddTask = onAddTask,
            onEditTask = { task, t, d, p, targetDate, reminderTimeMillis, repeatCount, subTasks, category, reminderType, replicateDates, everydayCount ->
                onEditTask(task, t, d, p, targetDate, reminderTimeMillis, repeatCount, subTasks, category, reminderType, replicateDates, everydayCount)
                onTaskToEditChange(null)
                Toast.makeText(context, "Task updated!", Toast.LENGTH_SHORT).show()
            },
            onTerminateForever = { task ->
                onTerminateForever?.invoke(task)
                onTaskToEditChange(null)
                Toast.makeText(context, "Sticky task terminated!", Toast.LENGTH_SHORT).show()
            },
            copiedTask = copiedTask,
            onCopy = onCopy,
            categories = categories,
            customCategories = customCategories,
            onAddCategory = onAddCustomCategory,
            onDeleteCategory = onDeleteCustomCategory
        )
    }

    // Delete Task Confirmation Dialog
    if (taskToDelete != null) {
        TaskDeleteDialog(
            taskToDelete = taskToDelete,
            colorSchemeType = colorSchemeType,
            onDismiss = { onTaskToDeleteChange(null) },
            onConfirmDelete = onDeleteTask
        )
    }
}

@Preview(showBackground = true, name = "HomeScreen - Empty State", showSystemUi = true)
@Composable
fun HomeScreenEmptyStatePreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        HomeScreenContent(
            currentDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            colorSchemeType = "minimal",
            showAddDialog = false,
            taskToEdit = null,
            taskToDelete = null,
            isAlarmPermissionGranted = true,
            isNotificationPermissionGranted = true,
            copiedTask = null,
            customCategories = listOf("Work", "Errands", "Fitness"),
            onShowAddDialogChange = {},
            onTaskToEditChange = {},
            onTaskToDeleteChange = {},
            onNavigateDate = {},
            onSetDate = {},
            onToggleComplete = {},
            onDeleteTask = {},
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onCopy = {},
            onAddCustomCategory = {},
            onDeleteCustomCategory = {},
            onToggleSubComplete = { _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(emptyList()) }
        )
    }
}

@Preview(showBackground = true, name = "HomeScreen - Dark Mode", showSystemUi = true)
@Composable
fun HomeScreenDarkModePreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "dark") {
        HomeScreenContent(
            currentDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            colorSchemeType = "minimal",
            showAddDialog = false,
            taskToEdit = null,
            taskToDelete = null,
            isAlarmPermissionGranted = true,
            isNotificationPermissionGranted = true,
            copiedTask = null,
            customCategories = listOf("Work", "Errands", "Fitness"),
            onShowAddDialogChange = {},
            onTaskToEditChange = {},
            onTaskToDeleteChange = {},
            onNavigateDate = {},
            onSetDate = {},
            onToggleComplete = {},
            onDeleteTask = {},
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onCopy = {},
            onAddCustomCategory = {},
            onDeleteCustomCategory = {},
            onToggleSubComplete = { _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(sampleTasks) }
        )
    }
}

@Preview(showBackground = true, name = "HomeScreen - Delete Dialog Visible", showSystemUi = true)
@Composable
fun HomeScreenDeleteDialogPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        HomeScreenContent(
            currentDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            colorSchemeType = "minimal",
            showAddDialog = false,
            taskToEdit = null,
            taskToDelete = sampleTasks[0],
            isAlarmPermissionGranted = true,
            isNotificationPermissionGranted = true,
            copiedTask = null,
            customCategories = listOf("Work", "Errands", "Fitness"),
            onShowAddDialogChange = {},
            onTaskToEditChange = {},
            onTaskToDeleteChange = {},
            onNavigateDate = {},
            onSetDate = {},
            onToggleComplete = {},
            onDeleteTask = {},
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onCopy = {},
            onAddCustomCategory = {},
            onDeleteCustomCategory = {},
            onToggleSubComplete = { _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(sampleTasks) }
        )
    }
}

@Preview(showBackground = true, name = "HomeScreen - Colorful Dark Theme", showSystemUi = true)
@Composable
fun HomeScreenColorfulDarkThemePreview() {
    SoftTodoTheme(colorSchemeType = "colorful", themeMode = "dark") {
        HomeScreenContent(
            currentDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            colorSchemeType = "colorful",
            showAddDialog = false,
            taskToEdit = null,
            taskToDelete = null,
            isAlarmPermissionGranted = true,
            isNotificationPermissionGranted = true,
            copiedTask = null,
            customCategories = listOf("Work", "Errands", "Fitness"),
            onShowAddDialogChange = {},
            onTaskToEditChange = {},
            onTaskToDeleteChange = {},
            onNavigateDate = {},
            onSetDate = {},
            onToggleComplete = {},
            onDeleteTask = {},
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onCopy = {},
            onAddCustomCategory = {},
            onDeleteCustomCategory = {},
            onToggleSubComplete = { _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(sampleTasks) }
        )
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
