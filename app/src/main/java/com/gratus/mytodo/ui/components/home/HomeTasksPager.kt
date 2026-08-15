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

package com.gratus.mytodo.ui.components.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.SortOption
import com.gratus.mytodo.ui.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

sealed interface HomeListItem {
    data class CategoryGroup(val category: String, val tasks: List<Task>) : HomeListItem
    data class TaglessTask(val task: Task) : HomeListItem
}

/**
 * Smooth sliding horizontal pager displaying daily task cards and category groups.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTasksPager(
    pagerState: PagerState,
    initialPage: Int,
    baseDate: Calendar,
    colorSchemeType: String,
    sortOption: SortOption,
    isStickyEnabled: Boolean,
    highlightedTaskId: Int? = null,
    onShowAddDialogChange: (Boolean) -> Unit,
    onTaskToEditChange: (Task?) -> Unit,
    onTaskToDeleteChange: (Task?) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onToggleSubComplete: (Task, Int) -> Unit,
    getTasksForDate: (String) -> Flow<List<Task>>,
    onPreselectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
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
                EmptyTasksState()
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
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                // Auto-expand category containing highlighted task
                androidx.compose.runtime.LaunchedEffect(highlightedTaskId, groupedTasks) {
                    if (highlightedTaskId != null) {
                        groupedTasks.forEach { (cat, tasks) ->
                            if (cat != null && tasks.any { it.id == highlightedTaskId }) {
                                collapsedCategories[cat] = false
                            }
                        }
                    }
                }

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

                // Scroll to highlighted task item
                LaunchedEffect(highlightedTaskId, homeListItems, stickyTasks) {
                    if (highlightedTaskId != null) {
                        val stickyIdx = stickyTasks.indexOfFirst { it.id == highlightedTaskId }
                        if (stickyIdx >= 0) {
                            listState.animateScrollToItem(0)
                        } else {
                            val itemIdx = homeListItems.indexOfFirst { item ->
                                when (item) {
                                    is HomeListItem.CategoryGroup -> item.tasks.any { it.id == highlightedTaskId }
                                    is HomeListItem.TaglessTask -> item.task.id == highlightedTaskId
                                }
                            }
                            if (itemIdx >= 0) {
                                val offset = if (stickyTasks.isNotEmpty()) 2 else 0
                                listState.animateScrollToItem(itemIdx + offset)
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
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
                                    onPreselectCategory("Sticky")
                                    onShowAddDialogChange(true)
                                },
                                colorSchemeType = colorSchemeType
                            ) {
                                stickyTasks.forEachIndexed { index, task ->
                                    TaskItemCard(
                                        task = task,
                                        colorSchemeType = colorSchemeType,
                                        isFlat = true,
                                        isHighlighted = task.id == highlightedTaskId,
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
                                thickness = 1.dp
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
                                        onPreselectCategory(catName)
                                        onShowAddDialogChange(true)
                                    },
                                    colorSchemeType = colorSchemeType
                                ) {
                                    item.tasks.forEachIndexed { index, task ->
                                        TaskItemCard(
                                            task = task,
                                            colorSchemeType = colorSchemeType,
                                            isFlat = true,
                                            isHighlighted = task.id == highlightedTaskId,
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
                                    isHighlighted = item.task.id == highlightedTaskId,
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

@OptIn(ExperimentalFoundationApi::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Home Tasks Pager")
@Composable
fun HomeTasksPagerPreview() {
    val sampleTasks = listOf(
        Task(id = 1, title = "Task Title 1", description = "Desc 1", priority = 1, dateAdded = "2026-08-13", category = "Work"),
        Task(id = 2, title = "Task Title 2", description = "Desc 2", priority = 2, dateAdded = "2026-08-13")
    )
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 500, pageCount = { 1000 })
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        HomeTasksPager(
            pagerState = pagerState,
            initialPage = 500,
            baseDate = Calendar.getInstance(),
            colorSchemeType = "minimal",
            sortOption = SortOption.PRIORITY,
            isStickyEnabled = true,
            onShowAddDialogChange = {},
            onTaskToEditChange = {},
            onTaskToDeleteChange = {},
            onToggleComplete = {},
            onToggleSubComplete = { _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(sampleTasks) },
            onPreselectCategory = {}
        )
    }
}
