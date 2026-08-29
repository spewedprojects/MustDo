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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.dialogContainerColor
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.Locale

/**
 * Custom Category creation AlertDialog.
 */
@Composable
fun AddCustomCategoryDialog(
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onCategorySelected: (String) -> Unit
) {
    var categoryInputText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                        onAddCategory(name)
                        onCategorySelected(name)
                    }
                    onDismiss()
                }
            ) {
                Text("Add", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

/**
 * Custom Date Range Picker Dialog wrapping Material 3 DateRangePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirmDates: (List<String>) -> Unit
) {
    val configuration = LocalConfiguration.current
    val defaultLocale = configuration.locales[0]

    // Force Monday as the first day of the week
    val customLocale = remember(defaultLocale) {
        Locale.forLanguageTag(defaultLocale.toLanguageTag() + "-u-fw-mon")
    }

    val dateRangePickerState = remember(customLocale) {
        DateRangePickerState(
            locale = customLocale
        )
    }

    val confirmEnabled by remember {
        derivedStateOf { dateRangePickerState.selectedStartDateMillis != null }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            shape = RoundedCornerShape(20.dp)
        ),
        confirmButton = {
            TextButton(
                enabled = confirmEnabled,
                onClick = {
                    dateRangePickerState.selectedStartDateMillis?.let { start ->
                        val dates = DateTimeUtils.expandDateRange(
                            start,
                            dateRangePickerState.selectedEndDateMillis
                        )
                        onConfirmDates(dates)
                    }
                    onDismiss()
                }
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.dialogContainerColor)
    )
    {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "Select Date Range",
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal
                )
            },
            headline = {
                DateRangePickerDefaults.DateRangePickerHeadline(
                    selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                    selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                    displayMode = dateRangePickerState.displayMode,
                    dateFormatter = DatePickerDefaults.dateFormatter(),
                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
                )
            },
            showModeToggle = false,
            modifier = Modifier.weight(1f),
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.dialogContainerColor,
                weekdayContentColor = MaterialTheme.colorScheme.onSurface)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Add Custom Category Dialog")
@Composable
fun AddCustomCategoryDialogPreview() {
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        AddCustomCategoryDialog(
            onDismiss = {},
            onAddCategory = {},
            onCategorySelected = {}
        )
    }
}
