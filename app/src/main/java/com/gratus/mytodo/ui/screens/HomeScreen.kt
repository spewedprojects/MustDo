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
import com.gratus.mytodo.ui.components.getCategoryIcon
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
    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
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
    val copiedTask by viewModel.copiedTask.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()

    val isStickyEnabled by viewModel.isStickyEnabled.collectAsState()

    HomeScreenContent(
        currentDate = currentDate,
        lastUsedPriority = lastUsedPriority,
        colorSchemeType = colorSchemeType,
        showAddDialog = showAddDialog,
        taskToEdit = taskToEdit,
        taskToDelete = taskToDelete,
        isAlarmPermissionGranted = isAlarmGranted,
        isNotificationPermissionGranted = isNotificationGranted,
        copiedTask = copiedTask,
        customCategories = customCategories,
        sortOption = sortOption,
        isStickyEnabled = isStickyEnabled,
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
        onEditTask = { task, t, d, p, targetDate, reminderTimeMillis, repeatCount, subTasks, category, reminderType ->
            viewModel.updateTaskFields(task.id, t, d, p, targetDate, reminderTimeMillis, repeatCount, subTasks, category, reminderType)
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
    copiedTask: CopiedTask?,
    customCategories: List<String>,
    sortOption: SortOption = SortOption.PRIORITY,
    isStickyEnabled: Boolean = true,
    onShowAddDialogChange: (Boolean) -> Unit = {},
    onTaskToEditChange: (Task?) -> Unit,
    onTaskToDeleteChange: (Task?) -> Unit,
    onNavigateDate: (Int) -> Unit,
    onSetDate: (Calendar) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTask: (String, String, Int, Calendar, List<String>, Int, Long?, Int, List<SubTask>, String?, String) -> Unit,
    onEditTask: (Task, String, String, Int, Calendar, Long?, Int, List<SubTask>, String?, String) -> Unit,
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

    // Smooth Sliding Pager setup
    val baseDate = remember { Calendar.getInstance() }
    val initialPage = 10000
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 20000 })

    val daysDiff = remember(currentDate) {
        DateTimeUtils.daysBetween(baseDate, currentDate)
    }
    val targetPage = initialPage + daysDiff

    // Scroll to page when currentDate changes externally (arrows, picker)
    LaunchedEffect(targetPage) {
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync focus date when page changes via swipe gesture
    LaunchedEffect(pagerState.currentPage) {
        val diff = pagerState.currentPage - initialPage
        val targetCal = (baseDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, diff)
        }
        if (!DateTimeUtils.isSameDay(targetCal, currentDate)) {
            onSetDate(targetCal)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isAlarmPermissionGranted || !isNotificationPermissionGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (colorSchemeType == "minimal") {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        } else if (colorSchemeType == "colorful") {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (colorSchemeType == "simple" || colorSchemeType == "minimal") {
                        androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (colorSchemeType == "simple") {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    } else {
                        null
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (colorSchemeType == "simple") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Reminder Notifications Disabled",
                                fontWeight = FontWeight.Bold,
                                color = if (colorSchemeType == "simple") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        
                        Text(
                            text = if (!isNotificationPermissionGranted && !isAlarmPermissionGranted) {
                                "Both Notification permission and Alarms & Reminders permission are required to trigger notifications for urgent scheduled tasks."
                            } else if (!isNotificationPermissionGranted) {
                                "Notification permission is required to trigger notifications for urgent scheduled tasks."
                            } else {
                                "Alarms & Reminders permission is required to schedule exact notifications for urgent tasks."
                            },
                            fontSize = AppFontSizes.small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = AppFontSizes.medium
                        )
                        
                        Button(
                            onClick = {
                                val intent = if (!isNotificationPermissionGranted) {
                                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                    }
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                    } else {
                                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                    }
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (colorSchemeType == "simple") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                contentColor = if (colorSchemeType == "simple") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Grant Permission", fontSize = AppFontSizes.small, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Screen Header Label Display - now redundant. TODO -> strip off in next release
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 2.dp)
//                    .clip(RoundedCornerShape(12.dp))
//                    .clickable { onToggleInlineCalendar() }
//                    .padding(horizontal = 8.dp, vertical = 6.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Row(
//                    horizontalArrangement = Arrangement.spacedBy(8.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.DateRange,
//                        contentDescription = "Pick a Date",
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.size(18.dp)
//                    )
//                    Text(
//                        text = DateTimeUtils.formatHomeDateLabel(currentDate),
//                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
//                        color = MaterialTheme.colorScheme.onSurface
//                    )
//                    Text(
//                        text = if (isInlineCalendarExpanded) " (Tap to Collapse)" else " (Tap for Calendar)",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
//                    )
//                }
//
//                Icon(
//                    imageVector = if (isInlineCalendarExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
//                    contentDescription = "Toggle Calendar",
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(20.dp)
//                )
//            }

            AnimatedVisibility(
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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageDiff = page - initialPage
                val pageDate = remember(page) {
                    (baseDate.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, pageDiff)
                    }
                }
                val dateStr = remember(pageDate) { DateTimeUtils.formatDbDate(pageDate) }
                val pageTasks by remember(dateStr) { getTasksForDate(dateStr) }
                    .collectAsState(initial = emptyList())

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (pageTasks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "No tasks",
                                    modifier = Modifier
                                        .size(82.dp)
                                        .alpha(0.3f),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "No tasks recorded for today",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Swipe horizontally or tap Quick-Add to start!",
                                    fontSize = AppFontSizes.small,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    } else {
                        val stickyTasks = remember(pageTasks, isStickyEnabled) {
                            if (!isStickyEnabled) emptyList()
                            else pageTasks.filter { it.category?.equals("Sticky", ignoreCase = true) == true }
                        }
                        val regularTasks = remember(pageTasks) {
                            pageTasks.filter { it.category?.equals("Sticky", ignoreCase = true) != true }
                        }
                        val groupedTasks = remember(regularTasks) {
                            regularTasks.groupBy { it.category }
                        }
                        val collapsedCategories = remember { mutableStateMapOf<String, Boolean>() }
                        val homeListItems = remember(groupedTasks, sortOption) {
                            val items = mutableListOf<HomeListItem>()
                            
                            groupedTasks.filterKeys { it != null }.forEach { (cat, tasks) ->
                                items.add(HomeListItem.CategoryGroup(cat!!, tasks))
                            }
                            
                            val uncategorizedTasks = groupedTasks[null] ?: emptyList()
                            uncategorizedTasks.forEach { task ->
                                items.add(HomeListItem.TaglessTask(task))
                            }
                            
                            if (sortOption == SortOption.PRIORITY) {
                                items.sortedWith(
                                    compareBy<HomeListItem> { item ->
                                        when (item) {
                                            is HomeListItem.CategoryGroup -> {
                                                val pending = item.tasks.filter { !it.isCompleted }
                                                pending.minOfOrNull { it.priority } ?: item.tasks.minOfOrNull { it.priority } ?: 4
                                            }
                                            is HomeListItem.TaglessTask -> {
                                                if (item.task.isCompleted) 5 else item.task.priority
                                            }
                                        }
                                    }.thenBy { item ->
                                        when (item) {
                                            is HomeListItem.CategoryGroup -> item.tasks.map { it.priority }.average()
                                            is HomeListItem.TaglessTask -> item.task.priority.toDouble()
                                        }
                                    }.thenBy { item ->
                                        when (item) {
                                            is HomeListItem.CategoryGroup -> item.category
                                            is HomeListItem.TaglessTask -> item.task.title
                                        }
                                    }
                                )
                            } else {
                                val (categories, tagless) = items.partition { it is HomeListItem.CategoryGroup }
                                val sortedCategories = categories.sortedBy { (it as HomeListItem.CategoryGroup).category }
                                val sortedTagless = tagless.sortedBy { (it as HomeListItem.TaglessTask).task.createdSeq }
                                sortedCategories + sortedTagless
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_tasks_list"),
                            contentPadding = PaddingValues(bottom = 80.dp), // Clear bottom FAB space
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Top Sticky Section
                            if (stickyTasks.isNotEmpty()) {
                                item(key = "sticky_section") {
                                    CategoryCard(
                                        category = "Sticky",
                                        tasks = stickyTasks,
                                        isExpanded = true,
                                        onToggleExpand = {},
                                        onQuickAdd = {
                                            preselectedCategory.value = "Sticky"
                                            onShowAddDialogChange(true)
                                        },
                                        colorSchemeType = colorSchemeType
                                    ) {
                                        stickyTasks.forEachIndexed { index, task ->
                                            TaskItemCard(
                                                task = task,
                                                colorSchemeType = colorSchemeType,
                                                isFlat = true,
                                                onToggleComplete = { onToggleComplete(task) },
                                                onDelete = { onTaskToDeleteChange(task) },
                                                onLongClick = { onTaskToEditChange(task) },
                                                onToggleSubComplete = { subIdx -> onToggleSubComplete(task, subIdx) }
                                            )
                                            if (index < stickyTasks.size - 1) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                    thickness = 1.dp
                                                )
                                            }
                                        }
                                    }
                                }
                                item(key = "sticky_divider") {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        thickness = 2.dp
                                    )
                                }
                            }
                            items(homeListItems, key = { item ->
                                when (item) {
                                    is HomeListItem.CategoryGroup -> "category_card_${item.category}"
                                    is HomeListItem.TaglessTask -> "task_${item.task.id}"
                                }
                            }) { item ->
                                when (item) {
                                    is HomeListItem.CategoryGroup -> {
                                        val catName = item.category
                                        val isCollapsed = collapsedCategories[catName] == true
                                        CategoryCard(
                                            category = catName,
                                            tasks = item.tasks,
                                            isExpanded = !isCollapsed,
                                            onToggleExpand = {
                                                collapsedCategories[catName] = !isCollapsed
                                            },
                                            onQuickAdd = {
                                                preselectedCategory.value = catName
                                                onShowAddDialogChange(true)
                                            },
                                            colorSchemeType = colorSchemeType
                                        ) {
                                            item.tasks.forEachIndexed { index, task ->
                                                TaskItemCard(
                                                    task = task,
                                                    colorSchemeType = colorSchemeType,
                                                    isFlat = true,
                                                    onToggleComplete = { onToggleComplete(task) },
                                                    onDelete = { onTaskToDeleteChange(task) },
                                                    onLongClick = { onTaskToEditChange(task) },
                                                    onToggleSubComplete = { subIdx -> onToggleSubComplete(task, subIdx) }
                                                )
                                                if (index < item.tasks.size - 1) {
                                                    HorizontalDivider(
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                        thickness = 1.dp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    is HomeListItem.TaglessTask -> {
                                        TaskItemCard(
                                            task = item.task,
                                            colorSchemeType = colorSchemeType,
                                            isFlat = false,
                                            onToggleComplete = { onToggleComplete(item.task) },
                                            onDelete = { onTaskToDeleteChange(item.task) },
                                            onLongClick = { onTaskToEditChange(item.task) },
                                            onToggleSubComplete = { subIdx -> onToggleSubComplete(item.task, subIdx) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Buttons (Add Task and optional Paste Task)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = copiedTask != null && copiedTask.originalDateAdded != DateTimeUtils.formatDbDate(currentDate),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FloatingActionButton(
                    onClick = {
                        copiedTask?.let { copied ->
                            val newReminderTime = copied.reminderTime?.let { origTime ->
                                val origCal = Calendar.getInstance().apply { timeInMillis = origTime }
                                val targetCal = Calendar.getInstance().apply {
                                    time = currentDate.time
                                    set(Calendar.HOUR_OF_DAY, origCal.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, origCal.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                targetCal.timeInMillis
                            }
                             onAddTask(
                                 copied.title,
                                 copied.description,
                                 copied.priority,
                                 currentDate,
                                 emptyList(),
                                 0,
                                 newReminderTime,
                                 copied.repeatCount,
                                 copied.subTasks,
                                 copied.category,
                                 copied.reminderType
                             )
                            Toast.makeText(context, "Task pasted!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("paste_task_fab")
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), shape = CircleShape),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste Task",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            FloatingActionButton(
                onClick = { onShowAddDialogChange(true) },
                modifier = Modifier.testTag("quick_add_fab"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
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
            onEditTask = { task, t, d, p, targetDate, reminderTimeMillis, repeatCount, subTasks, category, reminderType ->
                onEditTask(task, t, d, p, targetDate, reminderTimeMillis, repeatCount, subTasks, category, reminderType)
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
            customCategories = customCategories,
            onAddCategory = onAddCustomCategory,
            onDeleteCategory = onDeleteCustomCategory
        )
    }

    // Delete Task Confirmation Dialog
    if (taskToDelete != null) {
         AlertDialog(
            onDismissRequest = { onTaskToDeleteChange(null) },
            modifier = Modifier.border(
                width = 1.dp,
                color = if (colorSchemeType == "simple") {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                } else if (colorSchemeType == "minimal") {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(28.dp)
            ),
            containerColor = MaterialTheme.colorScheme.dialogContainerColor,
            title = { Text("Delete Task", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete this task?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskToDelete?.let { onDeleteTask(it) }
                        onTaskToDeleteChange(null)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onTaskToDeleteChange(null) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

/**
 * Task item card component.
 */
/**
 * Category group header card component.
 */
/**
 * Resolves color coding accents matching default and custom category titles.
 */
fun getCategoryAccentColor(category: String): Color {
    val lower = category.lowercase().trim()
    return when {
        lower.contains("work") || lower.contains("job") || lower.contains("office") || lower.contains("meet") || lower.contains("project") -> Color(0xFFE91E63) // Pink/Rose
        lower.contains("personal") || lower.contains("home") || lower.contains("self") || lower.contains("me") || lower.contains("private") -> Color(0xFF2196F3) // Blue
        lower.contains("errand") || lower.contains("shop") || lower.contains("buy") || lower.contains("grocer") || lower.contains("store") || lower.contains("market") -> Color(0xFF4CAF50) // Green
        lower.contains("gym") || lower.contains("workout") || lower.contains("exercise") || lower.contains("run") || lower.contains("fit") || lower.contains("sport") || lower.contains("fitness") || lower.contains("dumbbell") -> Color(0xFF9C27B0) // Purple
        lower.contains("health") || lower.contains("doctor") || lower.contains("hospital") || lower.contains("med") || lower.contains("medicine") || lower.contains("favorite") -> Color(0xFFFF5722) // Orange
        lower.contains("learn") || lower.contains("study") || lower.contains("book") || lower.contains("school") || lower.contains("class") || lower.contains("course") || lower.contains("read") -> Color(0xFFFFC107) // Amber/Yellow
        else -> Color(0xFF673AB7) // Indigo/default
    }
}

/**
 * Category group header card containing nested task items (single card mockup pattern).
 */
@Composable
fun CategoryCard(
    category: String,
    tasks: List<Task>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onQuickAdd: () -> Unit,
    colorSchemeType: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val accentColor = getCategoryAccentColor(category, colorSchemeType, isDark)
    val icon = getCategoryIcon(category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("category_card_$category"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = when (colorSchemeType) {
            "simple" -> androidx.compose.foundation.BorderStroke(
                1.dp, 
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            "minimal" -> androidx.compose.foundation.BorderStroke(
                1.dp, 
                if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder
            )
            "system" -> androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            else -> androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tasks.size.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(
                    onClick = { onQuickAdd() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Add in Category",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (isExpanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                content()
            }
        }
    }
}

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

/**
 * Custom Simple B&W border calculation.
 */
@Composable
fun borderStrokeSimple(isCompleted: Boolean): androidx.compose.foundation.BorderStroke {
    val color = if (isCompleted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    return androidx.compose.foundation.BorderStroke(1.dp, color)
}

/**
 * Returns prioritized color code from Urgent level 1 down to soft Low level 4.
 */
fun getPriorityBoxColor(priority: Int, isCompleted: Boolean): Color {
    if (isCompleted) return Color.LightGray.copy(alpha = 0.5f)
    return when (priority) {
        1 -> PriorityRed
        2 -> PriorityOrange
        3 -> PriorityAmber
        4 -> PriorityYellow
        else -> Color.Gray
    }
}

@Preview(showBackground = true, name = "HomeScreen - Minimal Theme", showSystemUi = true)
@Composable
fun HomeScreenMinimalPreview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
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
            onEditTask = { _, _, _, _, _, _, _, _, _, _ -> },
            onCopy = {},
            onAddCustomCategory = {},
            onDeleteCustomCategory = {},
            onToggleSubComplete = { _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(sampleTasks) }
        )
    }
}

@Preview(showBackground = true, name = "HomeScreen - Simple Theme")
@Composable
fun HomeScreenSimplePreview() {
    SoftTodoTheme(colorSchemeType = "simple") {
        HomeScreenContent(
            currentDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            colorSchemeType = "simple",
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
            onEditTask = { _, _, _, _, _, _, _, _, _, _ -> },
            onCopy = {},
            onAddCustomCategory = {},
            onDeleteCustomCategory = {},
            onToggleSubComplete = { _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(sampleTasks) }
        )
    }
}

@Preview(showBackground = true, name = "Task Item Card - Urgent")
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
            onEditTask = { _, _, _, _, _, _, _, _, _, _ -> },
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
            onEditTask = { _, _, _, _, _, _, _, _, _, _ -> },
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
            onEditTask = { _, _, _, _, _, _, _, _, _, _ -> },
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
            onEditTask = { _, _, _, _, _, _, _, _, _, _ -> },
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
