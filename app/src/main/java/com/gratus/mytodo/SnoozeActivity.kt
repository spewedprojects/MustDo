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

package com.gratus.mytodo

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.gratus.mytodo.components.NotificationReceiver
import com.gratus.mytodo.data.TaskDatabase
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.dialogContainerColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SnoozeActivity is a translucent dialog-themed Activity triggered from the reminder notification.
 * It presents preset snooze durations and a option for custom minute input.
 */
class SnoozeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taskId = intent.getIntExtra("task_id", -1)
        if (taskId == -1) {
            finish()
            return
        }

        // Dismiss the notification immediately when Snooze is pressed
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId)

        // Read active theme configurations
        val sharedPrefs = getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)
        val themeMode = sharedPrefs.getString("theme", "auto") ?: "auto"
        val colorSchemeType = sharedPrefs.getString("color_scheme", "minimal") ?: "minimal"

        setContent {
            SoftTodoTheme(
                themeMode = themeMode,
                colorSchemeType = colorSchemeType
            ) {
                SnoozeDialog(
                    colorSchemeType = colorSchemeType,
                    onSnooze = { mins -> performSnooze(taskId, mins) },
                    onDismiss = { performSnooze(taskId, 5) }
                )
            }
        }
    }

    private fun performSnooze(taskId: Int, snoozeMins: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = TaskDatabase.getDatabase(applicationContext)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null) {
                    // 1. Cancel currently scheduled repeat alarm
                    NotificationReceiver.cancelReminder(applicationContext, task)

                    // 2. Decrement repeatedTimes by 1 (clamped to 0) since snooze doesn't consume a repeat count
                    val prevRepeatedTimes = (task.repeatedTimes - 1).coerceAtLeast(0)

                    // 3. Compute target snooze alarm time
                    val snoozeUntil = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)

                    // 4. Update task details in DB
                    val updatedTask = task.copy(
                        repeatedTimes = prevRepeatedTimes,
                        nextReminderTime = snoozeUntil,
                        snoozedUntil = snoozeUntil
                    )
                    db.taskDao().updateTask(updatedTask)

                    // 6. Schedule exact alarm for snoozeUntil
                    NotificationReceiver.scheduleExactReminder(applicationContext, updatedTask)

                    // 7. Cancel the active notification
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(taskId)

                    // 8. Update Home Widget
                    val updateIntent = Intent("com.gratus.mytodo.action.WIDGET_UPDATE").apply {
                        setPackage(packageName)
                    }
                    sendBroadcast(updateIntent)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SnoozeActivity, "Snoozed for $snoozeMins minutes", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SnoozeActivity", "Error during snooze execution: ${e.message}", e)
            } finally {
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }
}

@Composable
fun SnoozeDialog(
    colorSchemeType: String,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }

    // Intercept back button to either navigate back to presets or perform fallback 5-min snooze
    BackHandler {
        if (showCustomInput) {
            showCustomInput = false
            customMinutesText = ""
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.border(
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
        title = {
            Text(
                text = if (showCustomInput) "Custom Snooze" else "Snooze Reminder",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            if (!showCustomInput) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose snooze duration:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Layout of preset times and custom button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(5, 10, 15, 30)
                        presets.forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onSnooze(mins) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        // Custom duration (+) button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { showCustomInput = true }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter duration in minutes:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                customMinutesText = input
                            }
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (showCustomInput) {
                TextButton(
                    onClick = {
                        val mins = customMinutesText.toIntOrNull()
                        if (mins != null && mins > 0) {
                            onSnooze(mins)
                        }
                    }
                ) {
                    Text("Set", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (showCustomInput) {
                        showCustomInput = false
                        customMinutesText = ""
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = if (showCustomInput) "Back" else "Cancel",
                    color = if (showCustomInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Preview(showBackground = true, name = "Snooze Dialog - Minimal Dark")
@Composable
fun SnoozeDialogMinimalDarkPreview() {
    SoftTodoTheme(themeMode = "dark", colorSchemeType = "minimal") {
        Box(modifier = Modifier.padding(16.dp)) {
            SnoozeDialog(
                colorSchemeType = "minimal",
                onSnooze = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Snooze Dialog - Simple Light")
@Composable
fun SnoozeDialogSimpleLightPreview() {
    SoftTodoTheme(themeMode = "light", colorSchemeType = "simple") {
        Box(modifier = Modifier.padding(16.dp)) {
            SnoozeDialog(
                colorSchemeType = "simple",
                onSnooze = {},
                onDismiss = {}
            )
        }
    }
}
