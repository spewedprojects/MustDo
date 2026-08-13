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

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.theme.dialogContainerColor

/**
 * Task deletion confirmation dialog matching theme borders.
 */
@Composable
fun TaskDeleteDialog(
    taskToDelete: Task,
    colorSchemeType: String,
    onDismiss: () -> Unit,
    onConfirmDelete: (Task) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onConfirmDelete(taskToDelete)
                    onDismiss()
                }
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Task Delete Dialog")
@Composable
fun TaskDeleteDialogPreview() {
    val sampleTask = Task(id = 1, title = "Task to Delete", description = "Test description", priority = 1, dateAdded = "2026-08-13")
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        TaskDeleteDialog(
            taskToDelete = sampleTask,
            colorSchemeType = "minimal",
            onDismiss = {},
            onConfirmDelete = {}
        )
    }
}
