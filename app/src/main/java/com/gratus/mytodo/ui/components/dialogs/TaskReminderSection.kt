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

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.Calendar

/**
 * Reminder notification setup section with time picker trigger, repeat segment control, and type selector.
 */
@Composable
fun TaskReminderSection(
    reminderTimestamp: Long?,
    reminderType: String,
    repeatCount: Int,
    initialDate: Calendar,
    context: Context,
    onReminderTimestampChange: (Long?) -> Unit,
    onReminderTypeChange: (String) -> Unit,
    onRepeatCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val showTimePicker = {
        val pickedCal = Calendar.getInstance().apply {
            time = initialDate.time
        }
        val currentCal = Calendar.getInstance()
        val defaultHour = if (reminderTimestamp != null) {
            Calendar.getInstance().apply { timeInMillis = reminderTimestamp }.get(Calendar.HOUR_OF_DAY)
        } else {
            currentCal.get(Calendar.HOUR_OF_DAY)
        }
        val defaultMinute = if (reminderTimestamp != null) {
            Calendar.getInstance().apply { timeInMillis = reminderTimestamp }.get(Calendar.MINUTE)
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
                    onReminderTimestampChange(pickedCal.timeInMillis)
                } else {
                    Toast.makeText(context, "Reminder must be set in the future!", Toast.LENGTH_SHORT).show()
                }
            },
            defaultHour,
            defaultMinute,
            android.text.format.DateFormat.is24HourFormat(context)
        ).show()
    }

    Column(modifier = modifier.fillMaxWidth()) {
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
                        "Trigger on: " + DateTimeUtils.formatAlarmDate(context, reminderTimestamp)
                    } else {
                        "Set an alert for this task"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (reminderTimestamp != null) {
                    IconButton(onClick = { onReminderTimestampChange(null) }) {
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
                                    onRepeatCountChange(if (repeatCount >= 4) 1 else repeatCount + 1)
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
            }
        }

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
                                .clickable { onReminderTypeChange(type) }
                                .padding(vertical = 6.dp),
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
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Task Reminder Section", backgroundColor = 0xfff)
@Composable
fun TaskReminderSectionPreview() {
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        TaskReminderSection(
            reminderTimestamp = System.currentTimeMillis() + 3600000,
            reminderType = "notification",
            repeatCount = 1,
            initialDate = Calendar.getInstance(),
            context = androidx.compose.ui.platform.LocalContext.current,
            onReminderTimestampChange = {},
            onReminderTypeChange = {},
            onRepeatCountChange = {}
        )
    }
}
