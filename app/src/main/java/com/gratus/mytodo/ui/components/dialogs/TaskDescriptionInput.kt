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

package com.gratus.mytodo.ui.components.dialogs

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.AppFontSizes

/**
 * Task description text field with contextual formatting toolbar (Bold, Italic, Bullet Point).
 */
@Composable
fun TaskDescriptionInput(
    descriptionValue: TextFieldValue,
    onDescriptionValueChange: (TextFieldValue) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = descriptionValue,
            onValueChange = onDescriptionValueChange,
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
                        onDescriptionValueChange(
                            TextFieldValue(
                                text = newText,
                                selection = TextRange(selection.start + 2, selection.start + 2 + selectedText.length)
                            )
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
                        onDescriptionValueChange(
                            TextFieldValue(
                                text = newText,
                                selection = TextRange(selection.start + 2, selection.start + 2 + selectedText.length)
                            )
                        )
                    } else {
                        Toast.makeText(context, "Select description text to format italic", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = MaterialTheme.colorScheme.primary)
            }

            // Bullet Point insertion button
            IconButton(
                onClick = {
                    val text = descriptionValue.text
                    val selection = descriptionValue.selection
                    val replacement = if (text.isEmpty() || text.endsWith("\n")) "- " else "\n- "
                    val newText = text.replaceRange(selection.start, selection.end, replacement)
                    onDescriptionValueChange(
                        TextFieldValue(
                            text = newText,
                            selection = TextRange(selection.start + replacement.length)
                        )
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
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Task Description Input")
@Composable
fun TaskDescriptionInputPreview() {
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        TaskDescriptionInput(
            descriptionValue = TextFieldValue("Sample task description details"),
            onDescriptionValueChange = {},
            context = androidx.compose.ui.platform.LocalContext.current
        )
    }
}
