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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.dialogContainerColor
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.Calendar
import java.util.TimeZone

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
    val dateRangePickerState = rememberDateRangePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
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
                        onConfirmDates(dates)
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
                        onConfirmDates(listOf(DateTimeUtils.formatDbDate(localCal)))
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
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f)
            )
        }
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
