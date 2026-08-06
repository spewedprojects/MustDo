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

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.CopiedTask
import com.gratus.mytodo.data.SubTask
import androidx.compose.ui.text.style.TextDecoration
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.theme.*
import com.gratus.mytodo.ui.utils.DateTimeUtils
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import java.util.*

/**
 * TaskAddDialog presents a rich Material 3 center dialog with complete task configurations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddDialog(
    initialDate: Calendar,
    lastUsedPriority: Int,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        priority: Int,
        targetDate: Calendar,
        replicateDates: List<String>,
        everydayCount: Int,
        reminderTimeMillis: Long?,
        repeatCount: Int,
        subTasks: List<SubTask>,
        category: String?,
        reminderType: String
    ) -> Unit,
    taskToEdit: Task? = null,
    preselectedCategory: String? = null,
    copiedTask: CopiedTask? = null,
    onCopy: ((CopiedTask) -> Unit)? = null,
    customCategories: List<String> = emptyList(),
    onAddCustomCategory: (String) -> Unit = {},
    onDeleteCustomCategory: (String) -> Unit = {}
) {
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf(taskToEdit?.title ?: "") }
    // Description text state holds TextFieldValue for selection range tracking & formatting injection.
    var descriptionValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { 
        mutableStateOf(TextFieldValue(taskToEdit?.description ?: "")) 
    }
    
    // Priority state defaults to last used priority
    var priority by rememberSaveable { mutableStateOf(taskToEdit?.priority ?: lastUsedPriority.coerceIn(1, 4)) }

    // Alarm reminder state (timestamp milliseconds)
    var reminderTimestamp by rememberSaveable { mutableStateOf<Long?>(taskToEdit?.reminderTime) }
    var reminderType by rememberSaveable { mutableStateOf(taskToEdit?.reminderType ?: "notification") }
    var repeatCount by rememberSaveable { mutableStateOf(taskToEdit?.repeatCount ?: 1) }

    var selectedCategory by remember { mutableStateOf(taskToEdit?.category ?: preselectedCategory) }
    var subTasksList by remember { mutableStateOf(taskToEdit?.subTasks ?: emptyList()) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Recurrence selection
    var everydayCount by rememberSaveable { mutableStateOf(0) } // 0 = none, 7 = week, 14, 30
    var replicationDates by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var showRangePickerDialog by rememberSaveable { mutableStateOf(false) }

    // Calendar instances for rendering next 7 days clone option
    val nextDays = remember {
        val list = mutableListOf<Calendar>()
        for (i in 1..3) {
            val d = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            list.add(d)
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("task_add_dialog_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.dialogContainerColor,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Elevation doesn't behave as intended when combined with a transparent bg.
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = descriptionValue,
                        onValueChange = { descriptionValue = it },
                        label = { Text("Description") },
                        placeholder = { Text("Details (lines starting with '- ' show as bullets)") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_desc_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Contextual Formatting utility buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Format Selection:",
                            fontSize = AppFontSizes.extraSmall,
                            modifier = Modifier.padding(start = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        // Bold Button
                        IconButton(
                            onClick = {
                                val text = descriptionValue.text
                                val selection = descriptionValue.selection
                                if (selection.start != selection.end) {
                                    val selectedText = text.substring(selection.start, selection.end)
                                    val formatted = "**$selectedText**"
                                    val newText = text.replaceRange(selection.start, selection.end, formatted)
                                    // Fix: Offset by 2 (for "**") and maintain selection length
                                    descriptionValue = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(selection.start + 2, selection.start + 2 + selectedText.length)
                                    )
                                } else {
                                    Toast.makeText(context, "Select description text to format bold", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Italic Button
                        IconButton(
                            onClick = {
                                val text = descriptionValue.text
                                val selection = descriptionValue.selection
                                if (selection.start != selection.end) {
                                    val selectedText = text.substring(selection.start, selection.end)
                                    val formatted = "__${selectedText}__"
                                    val newText = text.replaceRange(selection.start, selection.end, formatted)
                                    // Fix: Offset by 2 (for "__") and maintain selection length
                                    descriptionValue = TextFieldValue(
                                        text = newText,
                                        selection = TextRange(selection.start + 2, selection.start + 2 + selectedText.length))
                                } else {
                                    Toast.makeText(context, "Select description text to format italic", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Short-cut for Bullet Point insertion at cursor position
                        IconButton(
                            onClick = {
                                val text = descriptionValue.text
                                val selection = descriptionValue.selection
                                val replacement = if (text.isEmpty() || text.endsWith("\n")) "- " else "\n- "
                                val newText = text.replaceRange(selection.start, selection.end, replacement)
                                descriptionValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(selection.start + replacement.length)
                                )
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "Insert Bullet Point",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Balanced and beautiful Two-column layout: Priority selector on left, Category selector on right
                var showPriorityDropdown by remember { mutableStateOf(false) }
                var showCategoryDropdown by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column 1: Priority selector
                    Row(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showPriorityDropdown = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Priority Level",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (priority) {
                                    1 -> "Level 1: Urgent (Red)"
                                    2 -> "Level 2: High (Orange)"
                                    3 -> "Level 3: Medium (Amber)"
                                    4 -> "Level 4: Low (Yellow)"
                                    else -> "Level 1: Urgent (Red)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (priority) {
                                            1 -> PriorityRed
                                            2 -> PriorityOrange
                                            3 -> PriorityAmber
                                            4 -> PriorityYellow
                                            else -> Color.Gray
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = priority.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (priority == 4) Color.DarkGray else Color.White
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Priority",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showPriorityDropdown,
                            onDismissRequest = { showPriorityDropdown = false },
                            shape = RoundedCornerShape(10.dp),
                            containerColor = MaterialTheme.colorScheme.dialogContainerColor,
                            shadowElevation = 8.dp,
                        ) {
                            (1..4).forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (p) {
                                                1 -> "Level 1: Urgent (Red)"
                                                2 -> "Level 2: High (Orange)"
                                                3 -> "Level 3: Medium (Amber)"
                                                4 -> "Level 4: Low (Yellow)"
                                                else -> ""
                                            }
                                        )
                                    },
                                    onClick = {
                                        priority = p
                                        showPriorityDropdown = false
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when (p) {
                                                        1 -> PriorityRed
                                                        2 -> PriorityOrange
                                                        3 -> PriorityAmber
                                                        4 -> PriorityYellow
                                                        else -> Color.Gray
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = p.toString(),
                                                fontWeight = FontWeight.Bold,
                                                color = if (p == 4) Color.DarkGray else Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Vertical Divider
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight().width(1.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    // Column 2: Category Selector
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCategoryDropdown = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Category / Tags",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedCategory != null) {
                                    val cat = selectedCategory!!
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                            .clickable { selectedCategory = null }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "None",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Category",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )

                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            shape = RoundedCornerShape(10.dp),
                            containerColor = MaterialTheme.colorScheme.dialogContainerColor,
                            shadowElevation = 8.dp,
                            modifier = Modifier.heightIn(max = 350.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedCategory = null
                                    showCategoryDropdown = false
                                }
                            )
                            val defaultCats = listOf("Personal", "Work", "Errands", "Health", "Learning")
                            val allCategories = (defaultCats + customCategories).distinct()
                            allCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        showCategoryDropdown = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = getCategoryIcon(cat),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = if (customCategories.contains(cat)) {
                                        {
                                            IconButton(
                                                onClick = {
                                                    onDeleteCustomCategory(cat)
                                                    if (selectedCategory == cat) {
                                                        selectedCategory = null
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete custom tag",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else null
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Add custom category", color = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showCategoryDropdown = false
                                    showAddCategoryDialog = true
                                }
                            )
                        }
                    }
                }

                // Sub-tasks section
                var subTasksExpanded by remember { mutableStateOf(true) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subTasksExpanded = !subTasksExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sub-tasks",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${subTasksList.size}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = if (subTasksExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand/Collapse subtasks",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (subTasksExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp).animateContentSize(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                subTasksList.forEachIndexed { index, sub ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Reorder",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = sub.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                subTasksList = subTasksList.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete sub-task",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    if (index < subTasksList.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                }

                                // Add sub-task input row
                                var isAddingSubTask by remember { mutableStateOf(false) }
                                var newSubTaskTitle by remember { mutableStateOf("") }
                                val subTaskFocusRequester = remember { FocusRequester() }
                                
                                if (isAddingSubTask) {
                                    val keyboardController = LocalSoftwareKeyboardController.current
                                    LaunchedEffect(Unit) {
                                        delay(100)
                                        subTaskFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newSubTaskTitle,
                                            onValueChange = { newSubTaskTitle = it },
                                            placeholder = { Text("Enter sub-task...", fontSize = 14.sp) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(subTaskFocusRequester),
                                            shape = RoundedCornerShape(8.dp),
                                            keyboardOptions = KeyboardOptions(
                                                imeAction = ImeAction.Done,
                                                capitalization = KeyboardCapitalization.Sentences
                                            ),
                                            keyboardActions = KeyboardActions(onDone = {
                                                if (newSubTaskTitle.isNotBlank()) {
                                                    subTasksList = subTasksList + SubTask(newSubTaskTitle.trim())
                                                    newSubTaskTitle = ""
                                                    isAddingSubTask = false
                                                }
                                            })
                                        )
                                        IconButton(
                                            onClick = {
                                                if (newSubTaskTitle.isNotBlank()) {
                                                    subTasksList = subTasksList + SubTask(newSubTaskTitle.trim())
                                                    newSubTaskTitle = ""
                                                }
                                                isAddingSubTask = false
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Save",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                newSubTaskTitle = ""
                                                isAddingSubTask = false
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isAddingSubTask = true }
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add sub-task",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Add sub-task",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                    HorizontalDivider(
                        Modifier,
                        DividerDefaults.Thickness,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Reminder notification setup for urgent tasks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reminder Notification",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (reminderTimestamp != null) {
                                "Trigger on: " + DateTimeUtils.formatAlarmDate(context, reminderTimestamp!!)
                            } else {
                                "Set an alert for this task"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val showTimePicker = {
                        val pickedCal = Calendar.getInstance().apply {
                            time = initialDate.time
                        }
                        val currentCal = Calendar.getInstance()
                        val defaultHour = if (reminderTimestamp != null) {
                            Calendar.getInstance().apply { timeInMillis = reminderTimestamp!! }.get(Calendar.HOUR_OF_DAY)
                        } else {
                            currentCal.get(Calendar.HOUR_OF_DAY)
                        }
                        val defaultMinute = if (reminderTimestamp != null) {
                            Calendar.getInstance().apply { timeInMillis = reminderTimestamp!! }.get(Calendar.MINUTE)
                        } else {
                            currentCal.get(Calendar.MINUTE)
                        }

                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                pickedCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                pickedCal.set(Calendar.MINUTE, minute)
                                pickedCal.set(Calendar.SECOND, 0)
                                pickedCal.set(Calendar.MILLISECOND, 0)
                                
                                if (pickedCal.timeInMillis > System.currentTimeMillis()) {
                                    reminderTimestamp = pickedCal.timeInMillis
                                } else {
                                    Toast.makeText(context, "Reminder must be set in the future!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            defaultHour,
                            defaultMinute,
                            android.text.format.DateFormat.is24HourFormat(context)
                        ).show()
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (reminderTimestamp != null) {
                            IconButton(onClick = { reminderTimestamp = null }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear reminder",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        if (reminderTimestamp == null) {
                            Button(
                                onClick = { showTimePicker() }
                            ) {
                                Text("Schedule")
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clip(RoundedCornerShape(20.dp)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Edit Segment
                                Box(
                                    modifier = Modifier
                                        .clickable { showTimePicker() }
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Edit",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                // Separator line
                                HorizontalDivider(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .width(1.dp),
                                    thickness = DividerDefaults.Thickness,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // Cycle Repeat Segment
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            repeatCount = if (repeatCount >= 4) 1 else repeatCount + 1
                                        }
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${repeatCount}x",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }}

                if (reminderTimestamp != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Reminder Type",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val types = listOf(
                                Pair("notification", "Notification"),
                                Pair("alarm", "Alarm")
                            )
                            types.forEach { (type, label) ->
                                val selected = reminderType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { reminderType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        textAlign = TextAlign.Center,
                                        fontSize = AppFontSizes.extraSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer 
                                                else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                if (taskToEdit == null) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Everyday recurrence auto-add option
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Auto-Add Everyday",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val intervals = listOf(
                                Pair(0, "No"),
                                Pair(7, "Next 7 Days"),
                                Pair(14, "Next 14 Days"),
                                Pair(30, "Next 30 Days")
                            )

                            intervals.forEach { (days, label) ->
                                val selected = everydayCount == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { everydayCount = days }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        textAlign = TextAlign.Center,
                                        fontSize = AppFontSizes.extraSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer 
                                                else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Future Clone Dates Multiselection Bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Also Add to Custom Future Dates",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(nextDays) { dayCal ->
                                val dbStr = DateTimeUtils.formatDbDate(dayCal)
                                val label = DateTimeUtils.formatAddDialogDay(dayCal)
                                val isSelected = replicationDates.contains(dbStr)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            replicationDates = if (isSelected) {
                                                replicationDates - dbStr
                                            } else {
                                                replicationDates + dbStr
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = AppFontSizes.small,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            item {
                                val isRangeActive = replicationDates.any { dateStr ->
                                    !nextDays.any { DateTimeUtils.formatDbDate(it) == dateStr }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isRangeActive) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isRangeActive) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { showRangePickerDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = if (isRangeActive) MaterialTheme.colorScheme.onSecondary 
                                                   else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isRangeActive) "Range Selected" else "+ Custom Range...",
                                            fontSize = AppFontSizes.small,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRangeActive) MaterialTheme.colorScheme.onSecondary 
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                } // End of scrollable Column

                Spacer(modifier = Modifier.height(8.dp))

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
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "Please enter a task title", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onConfirm(
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
                        },
                        modifier = Modifier.testTag("task_confirm_button")
                    ) {
                        Text(if (taskToEdit == null) "Add Task" else "Save Changes")
                    }
                }
            }
        }
    }

    // Custom Category Name Input Dialog
    if (showAddCategoryDialog) {
        var categoryInputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(28.dp)
            ),
            containerColor = MaterialTheme.colorScheme.dialogContainerColor,
            title = { Text("Add Custom Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Enter a name for the new custom tag:")
                    OutlinedTextField(
                        value = categoryInputText,
                        onValueChange = { categoryInputText = it },
                        placeholder = { Text("e.g., Gym Workout") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = categoryInputText.trim()
                        if (name.isNotBlank()) {
                            onAddCustomCategory(name)
                            selectedCategory = name
                        }
                        showAddCategoryDialog = false
                    }
                ) {
                    Text("Add", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Custom Date Range Picker Dialog for replication selection
    if (showRangePickerDialog) {
        val dateRangePickerState = rememberDateRangePickerState()

        DatePickerDialog(
            onDismissRequest = { showRangePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            // Extract year, month, and day in UTC timezone to prevent offset warping
                            val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startMillis }
                            val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = endMillis }

                            val dates = mutableListOf<String>()
                            val cursor = startCal.clone() as Calendar
                            while (!cursor.after(endCal)) {
                                val localCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, cursor.get(Calendar.YEAR))
                                    set(Calendar.MONTH, cursor.get(Calendar.MONTH))
                                    set(Calendar.DAY_OF_MONTH, cursor.get(Calendar.DAY_OF_MONTH))
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                dates.add(DateTimeUtils.formatDbDate(localCal))
                                cursor.add(Calendar.DAY_OF_YEAR, 1)
                            }
                            replicationDates = dates
                        } else if (startMillis != null) {
                            val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startMillis }
                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, startCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, startCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, startCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            replicationDates = listOf(DateTimeUtils.formatDbDate(localCal))
                        }
                        showRangePickerDialog = false
                    }
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangePickerDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp
        ) {
            // We use a Column to wrap the picker to prevent the layout from "warping"
            // within the Dialog's constraints.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp), // Height limit fixes the stretched look
                    title = {
                        Text(
                            text = "Select Range",
                            modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    // Removed the manual 'headline' block.
                    // The picker will now automatically show "Start Date - End Date" correctly.

                    // Fixed the color parameters to use standard M3 ColorScheme
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                        dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                        dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSurface,
                        todayContentColor = MaterialTheme.colorScheme.primary,
                        todayDateBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

/**
 * Helper to match category string with its designated icon.
 */
@Composable
fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = category.lowercase().trim()
    return when {
        lower.contains("work") || lower.contains("job") || lower.contains("office") || lower.contains("meet") || lower.contains("project") -> Icons.Default.Work
        lower.contains("personal") || lower.contains("home") || lower.contains("self") || lower.contains("me") || lower.contains("private") -> Icons.Default.Person
        lower.contains("errand") || lower.contains("shop") || lower.contains("buy") || lower.contains("grocer") || lower.contains("store") || lower.contains("market") -> Icons.Default.ShoppingCart
        lower.contains("gym") || lower.contains("workout") || lower.contains("exercise") || lower.contains("run") || lower.contains("fit") || lower.contains("sport") || lower.contains("fitness") || lower.contains("dumbbell") -> Icons.Default.FitnessCenter
        lower.contains("health") || lower.contains("doctor") || lower.contains("hospital") || lower.contains("med") || lower.contains("medicine") || lower.contains("favorite") -> Icons.Default.Favorite
        lower.contains("learn") || lower.contains("study") || lower.contains("book") || lower.contains("school") || lower.contains("class") || lower.contains("course") || lower.contains("read") -> Icons.Default.School
        else -> Icons.Default.Sell
    }
}

/**
 * Visual chip for displaying and selecting categories. Keep in case you want to remvoe the dropdown menu later
 */
@Composable
fun CategoryChip(
    category: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = getCategoryIcon(category),
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = category,
            fontSize = AppFontSizes.extraSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        if (onDelete != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete custom tag",
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

// --- Previews ---

@Preview(showBackground = true, name = "New Task - Minimal Light")
@Composable
fun TaskAddDialogNewTaskPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        TaskAddDialog(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Edit Task - Minimal Dark")
@Composable
fun TaskAddDialogEditTaskPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "dark") {
        TaskAddDialog(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _, _, _, _, _, _ -> },
            taskToEdit = previewTask
        )
    }
}

@Preview(showBackground = true, name = "Task with Subtasks State")
@Composable
fun TaskAddDialogSubtasksPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        TaskAddDialog(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _, _, _, _, _, _ -> },
            taskToEdit = previewTask.copy(
                subTasks = listOf(
                    SubTask("Completed Subtask", true),
                    SubTask("Pending Subtask", false),
                    SubTask("Another Pending One", false)
                )
            )
        )
    }
}

@Preview(showBackground = true, name = "New Task - Simple Theme")
@Composable
fun TaskAddDialogSimplePreview() {
    SoftTodoTheme(colorSchemeType = "simple", themeMode = "light") {
        TaskAddDialog(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 2,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "New Task - Colorful Dark Theme")
@Composable
fun TaskAddDialogColorfulDarkPreview() {
    SoftTodoTheme(colorSchemeType = "colorful", themeMode = "dark") {
        TaskAddDialog(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 3,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Edit Task - Recurring Alarm")
@Composable
fun TaskAddDialogRecurringAlarmPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        TaskAddDialog(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _, _, _, _, _, _ -> },
            taskToEdit = previewTask.copy(
                isRecurring = true,
                repeatCount = 3,
                reminderType = "alarm",
                title = "Critical System Maintenance"
            )
        )
    }
}

@Preview(showBackground = true, name = "New Task - High Priority Light")
@Composable
fun TaskAddDialogHighPriorityPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        TaskAddDialog(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Edit Task - Scheduled Task")
@Composable
fun TaskAddDialogScheduledTaskPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        TaskAddDialog(
            initialDate = Calendar.getInstance(),
            lastUsedPriority = 2,
            onDismiss = {},
            onConfirm = { _, _, _, _, _, _, _, _, _, _, _ -> },
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
