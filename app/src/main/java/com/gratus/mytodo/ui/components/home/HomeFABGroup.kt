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

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.data.CopiedTask
import com.gratus.mytodo.data.SubTask
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.Calendar

/**
 * Floating Action Buttons for Paste Task and Quick Add Task.
 */
@Composable
fun HomeFABGroup(
    copiedTask: CopiedTask?,
    currentDate: Calendar,
    context: Context,
    onShowAddDialogChange: (Boolean) -> Unit,
    onAddTask: (String, String, Int, Calendar, List<String>, Int, Long?, Int, List<SubTask>, String?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Home FAB Group")
@Composable
fun HomeFABGroupPreview() {
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        HomeFABGroup(
            copiedTask = CopiedTask("Task Title", "Task Desc", 1),
            currentDate = Calendar.getInstance(),
            context = androidx.compose.ui.platform.LocalContext.current,
            onShowAddDialogChange = {},
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}
