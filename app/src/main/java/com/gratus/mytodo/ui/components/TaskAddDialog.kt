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

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gratus.mytodo.data.CopiedTask
import com.gratus.mytodo.data.SubTask
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.components.dialogs.AddCustomCategoryDialog
import com.gratus.mytodo.ui.components.dialogs.CustomDateRangePickerDialog
import com.gratus.mytodo.ui.components.dialogs.MultiDayReplicationSection
import com.gratus.mytodo.ui.components.dialogs.SubTaskSection
import com.gratus.mytodo.ui.components.dialogs.TaskDescriptionInput
import com.gratus.mytodo.ui.components.dialogs.TaskPriorityCategoryRow
import com.gratus.mytodo.ui.components.dialogs.TaskReminderSection
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.dialogContainerColor
import java.util.Calendar

/**
 * TaskAddDialog presents a rich Material 3 center dialog with complete task configurations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddDialog(
    initialDate: Calendar,
    lastUsedPriority: Int,
    colorSchemeType: String = "minimal",
    onDismiss: () -> Unit,
    onAddTask: (String, String, Int, Calendar, List<String>, Int, Long?, Int, List<SubTask>, String?, String) -> Unit,
    onEditTask: (Task, String, String, Int, Calendar, Long?, Int, List<SubTask>, String?, String, List<String>, Int) -> Unit,
    onTerminateForever: ((Task) -> Unit)? = null,
    taskToEdit: Task? = null,
    preselectedCategory: String? = null,
    copiedTask: CopiedTask? = null,
    onCopy: ((CopiedTask) -> Unit)? = null,
    categories: List<String> = emptyList(),
    customCategories: List<String> = emptyList(),
    onAddCategory: (String) -> Unit = {},
    onDeleteCategory: (String) -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        TaskAddDialogContent(
            initialDate = initialDate,
            lastUsedPriority = lastUsedPriority,
            colorSchemeType = colorSchemeType,
            onDismiss = onDismiss,
            onAddTask = onAddTask,
            onEditTask = onEditTask,
            onTerminateForever = onTerminateForever,
            taskToEdit = taskToEdit,
            preselectedCategory = preselectedCategory,
            copiedTask = copiedTask,
            onCopy = onCopy,
            categories = categories,
            customCategories = customCategories,
            onAddCategory = onAddCategory,
            onDeleteCategory = onDeleteCategory
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddDialogContent(
    initialDate: Calendar,
    lastUsedPriority: Int,
    colorSchemeType: String = "minimal",
    onDismiss: () -> Unit,
    onAddTask: (String, String, Int, Calendar, List<String>, Int, Long?, Int, List<SubTask>, String?, String) -> Unit,
    onEditTask: (Task, String, String, Int, Calendar, Long?, Int, List<SubTask>, String?, String, List<String>, Int) -> Unit,
    onTerminateForever: ((Task) -> Unit)? = null,
    taskToEdit: Task? = null,
    preselectedCategory: String? = null,
    copiedTask: CopiedTask? = null,
    onCopy: ((CopiedTask) -> Unit)? = null,
    categories: List<String> = emptyList(),
    customCategories: List<String> = emptyList(),
    onAddCategory: (String) -> Unit = {},
    onDeleteCategory: (String) -> Unit = {}
) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf(taskToEdit?.title ?: "") }
    var descriptionValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { 
        mutableStateOf(TextFieldValue(taskToEdit?.description ?: "")) 
    }
    
    var priority by rememberSaveable { mutableStateOf(taskToEdit?.priority ?: lastUsedPriority.coerceIn(1, 4)) }
    var reminderTimestamp by rememberSaveable { mutableStateOf<Long?>(taskToEdit?.reminderTime) }
    var reminderType by rememberSaveable { mutableStateOf(taskToEdit?.reminderType ?: "notification") }
    var repeatCount by rememberSaveable { mutableStateOf(taskToEdit?.repeatCount ?: 1) }

    var subTasksList by remember { mutableStateOf(taskToEdit?.subTasks ?: emptyList()) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    var everydayCount by rememberSaveable { mutableStateOf(0) }
    var replicationDates by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showRangePickerDialog by rememberSaveable { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf(taskToEdit?.category ?: preselectedCategory) }
    val isStickyCategory = selectedCategory?.equals("Sticky", ignoreCase = true) == true

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 16.dp)
            .testTag("task_add_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(0.5f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.dialogContainerColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (taskToEdit == null) "New Must-Do Task" else "Edit Task",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Dialog")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("What needs to be done?") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )

                    // Description Input & Custom Formatting Toolbar
                    TaskDescriptionInput(
                        descriptionValue = descriptionValue,
                        onDescriptionValueChange = { descriptionValue = it },
                        context = context
                    )

                    // Priority Level & Category / Tag Selector Row
                    TaskPriorityCategoryRow(
                        priority = priority,
                        selectedCategory = selectedCategory,
                        categories = categories,
                        customCategories = customCategories,
                        onPriorityChange = { priority = it },
                        onCategoryChange = { selectedCategory = it },
                        onShowAddCategoryDialog = { showAddCategoryDialog = true },
                        onDeleteCategory = onDeleteCategory
                    )

                    val wasStickyOriginal = taskToEdit?.category?.equals("Sticky", ignoreCase = true) == true
                    if (taskToEdit != null && wasStickyOriginal && !isStickyCategory) {
                        Text(
                            text = "Note: Changing to a regular category will make this a single-day task on this date and stop future daily recurrence.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Sub-tasks Section
                    SubTaskSection(
                        subTasksList = subTasksList,
                        onSubTasksChange = { subTasksList = it }
                    )

                    // Reminder Notification Setup Section
                    TaskReminderSection(
                        reminderTimestamp = reminderTimestamp,
                        reminderType = reminderType,
                        repeatCount = repeatCount,
                        initialDate = initialDate,
                        context = context,
                        onReminderTimestampChange = { reminderTimestamp = it },
                        onReminderTypeChange = { reminderType = it },
                        onRepeatCountChange = { repeatCount = it }
                    )

                    // Multi-day Replication Controls (shown for creation mode OR when converting a non-sticky task to Sticky in edit mode)
                    val showReplicationSection = (taskToEdit == null) || (!wasStickyOriginal && isStickyCategory)
                    if (showReplicationSection) {
                        MultiDayReplicationSection(
                            isStickyCategory = isStickyCategory,
                            everydayCount = everydayCount,
                            replicationDates = replicationDates,
                            onEverydayCountChange = { everydayCount = it },
                            onReplicationDatesChange = { replicationDates = it },
                            onShowRangePickerDialog = { showRangePickerDialog = true }
                        )
                    }
                } // End of scrollable Column

                // Terminate Sticky Task Forever Button
                if (taskToEdit != null && isStickyCategory && onTerminateForever != null) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Button(
                        onClick = {
                            onTerminateForever(taskToEdit)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mark Complete Forever (Terminate)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(0.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (taskToEdit != null) {
                        TextButton(
                            onClick = {
                                onCopy?.invoke(
                                    CopiedTask(
                                        title = title,
                                        description = descriptionValue.text,
                                        priority = priority,
                                        reminderTime = reminderTimestamp,
                                        repeatCount = repeatCount,
                                        subTasks = subTasksList,
                                        category = selectedCategory,
                                        reminderType = reminderType,
                                        originalDateAdded = taskToEdit.dateAdded
                                    )
                                )
                                Toast.makeText(context, "Task copied!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Task",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Copy")
                            }
                        }
                    } else if (copiedTask != null) {
                        TextButton(
                            onClick = {
                                title = copiedTask.title
                                descriptionValue = TextFieldValue(copiedTask.description)
                                priority = copiedTask.priority
                                reminderTimestamp = copiedTask.reminderTime?.let { origTime ->
                                    val origCal = Calendar.getInstance().apply { timeInMillis = origTime }
                                    val targetCal = Calendar.getInstance().apply {
                                        time = initialDate.time
                                        set(Calendar.HOUR_OF_DAY, origCal.get(Calendar.HOUR_OF_DAY))
                                        set(Calendar.MINUTE, origCal.get(Calendar.MINUTE))
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    targetCal.timeInMillis
                                }
                                repeatCount = copiedTask.repeatCount
                                subTasksList = copiedTask.subTasks
                                selectedCategory = copiedTask.category
                                reminderType = copiedTask.reminderType
                                Toast.makeText(context, "Task pasted!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste Task",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Paste")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Cancel & Confirm buttons
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Please enter a task title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (taskToEdit == null) {
                                onAddTask(
                                    title.trim(),
                                    descriptionValue.text.trim(),
                                    priority,
                                    initialDate,
                                    replicationDates.toList(),
                                    everydayCount,
                                    reminderTimestamp,
                                    repeatCount,
                                    subTasksList,
                                    selectedCategory,
                                    reminderType
                                )
                            } else {
                                onEditTask(
                                    taskToEdit,
                                    title.trim(),
                                    descriptionValue.text.trim(),
                                    priority,
                                    initialDate,
                                    reminderTimestamp,
                                    repeatCount,
                                    subTasksList,
                                    selectedCategory,
                                    reminderType,
                                    replicationDates.toList(),
                                    everydayCount
                                )
                            }
                        },
                        modifier = Modifier.testTag("task_confirm_button")
                    ) {
                        Text(if (taskToEdit == null) "Add Task" else "Save Changes")
                    }
                }
            }
        }

    // Custom Category Name Input Dialog
    if (showAddCategoryDialog) {
        AddCustomCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAddCategory = onAddCategory,
            onCategorySelected = { selectedCategory = it }
        )
    }

    // Custom Date Range Picker Dialog for replication selection
    if (showRangePickerDialog) {
        CustomDateRangePickerDialog(
            onDismiss = { showRangePickerDialog = false },
            onConfirmDates = { dates -> replicationDates = dates }
        )
    }
}

@Preview(showBackground = true, name = "New Task Dialog - Minimal Light")
@Composable
fun TaskAddDialogMinimalLightPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "dark") {
        TaskAddDialogContent(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            onDismiss = {},
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Edit Task - Scheduled Task")
@Composable
fun TaskAddDialogScheduledTaskPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        TaskAddDialogContent(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 2,
            onDismiss = {},
            onAddTask = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
            taskToEdit = previewTask.copy(
                title = "Team Sync Meeting",
                description = "Discuss weekly progress and updates",
                reminderTime = System.currentTimeMillis() + 3600000,
                reminderType = "notification"
            )
        )
    }
}

private val previewTask = Task(
    id = 101,
    title = "Buy Groceries",
    description = "Need to get some fresh vegetables and fruits.\n- Spinach\n- Apples\n- Bananas",
    priority = 2,
    dateAdded = "2023-11-05",
    isCompleted = false,
    reminderTime = System.currentTimeMillis() + 7200000,
    repeatCount = 1,
    subTasks = listOf(
        SubTask("Spinach", true),
        SubTask("Apples", false),
        SubTask("Bananas", false)
    ),
    category = "Errands"
)
