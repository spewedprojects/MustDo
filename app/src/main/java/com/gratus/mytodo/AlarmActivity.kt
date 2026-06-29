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
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.components.AlarmService
import com.gratus.mytodo.components.AlarmState
import com.gratus.mytodo.components.NotificationReceiver
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.TaskDatabase
import com.gratus.mytodo.ui.theme.PriorityRed
import com.gratus.mytodo.ui.theme.PriorityOrange
import com.gratus.mytodo.ui.theme.PriorityAmber
import com.gratus.mytodo.ui.theme.PriorityYellow
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.dialogContainerColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock screen presentation configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        or android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val sharedPrefs = getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)
        val themeMode = sharedPrefs.getString("theme", "auto") ?: "auto"
        val colorSchemeType = sharedPrefs.getString("color_scheme", "minimal") ?: "minimal"

        setContent {
            SoftTodoTheme(
                themeMode = themeMode,
                colorSchemeType = colorSchemeType
            ) {
                LaunchedEffect(AlarmState.activeTasks.size) {
                    if (AlarmState.activeTasks.isEmpty()) {
                        finish()
                    }
                }

                AlarmScreen(
                    activeTasks = AlarmState.activeTasks,
                    onCompleteTask = { completeTask(it) },
                    onSnoozeTask = { task, mins ->
                        NotificationReceiver.performSnooze(applicationContext, task.id, mins)
                    },
                    onStopTaskAlarm = { stopTaskAlarm(it) },
                    onBackFallback = { snoozeAllRemaining() }
                )
            }
        }
    }

    private fun completeTask(task: Task) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = TaskDatabase.getDatabase(applicationContext)
            val updated = task.copy(isCompleted = true, snoozedUntil = null)
            db.taskDao().updateTask(updated)

            NotificationReceiver.cancelReminder(applicationContext, updated)
            NotificationReceiver.cancelAlarmTimeout(applicationContext, task.id)

            // Stop service alarm for this task
            val serviceIntent = Intent(applicationContext, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP_ALARM
                putExtra("task_id", task.id)
            }
            startService(serviceIntent)

            // Dismiss notification
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(task.id)

            // Update widgets
            val updateIntent = Intent("com.gratus.mytodo.action.WIDGET_UPDATE").apply {
                setPackage(packageName)
            }
            sendBroadcast(updateIntent)
        }
    }

    private fun stopTaskAlarm(task: Task) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = TaskDatabase.getDatabase(applicationContext)
            val updated = task.copy(isReminderActive = false, repeatedTimes = 0, nextReminderTime = null, snoozedUntil = null)
            db.taskDao().updateTask(updated)

            NotificationReceiver.cancelReminder(applicationContext, task)
            NotificationReceiver.cancelAlarmTimeout(applicationContext, task.id)

            // Stop service alarm for this task
            val serviceIntent = Intent(applicationContext, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP_ALARM
                putExtra("task_id", task.id)
            }
            startService(serviceIntent)

            // Dismiss notification
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(task.id)

            // Update widgets
            val updateIntent = Intent("com.gratus.mytodo.action.WIDGET_UPDATE").apply {
                setPackage(packageName)
            }
            sendBroadcast(updateIntent)
        }
    }

    private fun snoozeAllRemaining() {
        val remainingTasks = ArrayList(AlarmState.activeTasks)
        if (remainingTasks.isNotEmpty()) {
            for (task in remainingTasks) {
                NotificationReceiver.performSnooze(applicationContext, task.id, 5)
            }
        }
        finish()
    }

    override fun onDestroy() {
        // If there are still active tasks that weren't actioned, perform fallback snooze
        val remainingTasks = ArrayList(AlarmState.activeTasks)
        if (remainingTasks.isNotEmpty()) {
            for (task in remainingTasks) {
                NotificationReceiver.performSnooze(applicationContext, task.id, 5)
            }
        }
        super.onDestroy()
    }
}

@Composable
fun AlarmScreen(
    activeTasks: List<Task>,
    onCompleteTask: (Task) -> Unit,
    onSnoozeTask: (Task, Int) -> Unit,
    onStopTaskAlarm: (Task) -> Unit,
    onBackFallback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSnoozeTaskId by remember { mutableStateOf<Int?>(null) }
    var showCustomSnooze by remember { mutableStateOf(false) }
    var customSnoozeMinutesText by remember { mutableStateOf("") }

    BackHandler {
        if (activeSnoozeTaskId != null) {
            if (showCustomSnooze) {
                showCustomSnooze = false
                customSnoozeMinutesText = ""
            } else {
                activeSnoozeTaskId = null
            }
        } else {
            onBackFallback()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulse-like icon at the top
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Alarm Fired",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "CRITICAL TASK ALARM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Render each task in list/stack
            activeTasks.forEach { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(
                            width = 1.dp,
                            color = when (task.priority) {
                                1 -> PriorityRed
                                2 -> PriorityOrange
                                3 -> PriorityAmber
                                4 -> PriorityYellow
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (activeSnoozeTaskId == task.id) {
                            // Inline Snooze Picker
                            if (!showCustomSnooze) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Select Snooze Duration",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
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
                                                    .clickable {
                                                        onSnoozeTask(task, mins)
                                                    }
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${mins}m",
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .clickable { showCustomSnooze = true }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { activeSnoozeTaskId = null }) {
                                        Text("Cancel")
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    OutlinedTextField(
                                        value = customSnoozeMinutesText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                                customSnoozeMinutesText = input
                                            }
                                        },
                                        label = { Text("Minutes") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        TextButton(onClick = {
                                            showCustomSnooze = false
                                            customSnoozeMinutesText = ""
                                        }) {
                                            Text("Back")
                                        }
                                        Button(onClick = {
                                            val mins = customSnoozeMinutesText.toIntOrNull()
                                            if (mins != null && mins > 0) {
                                                onSnoozeTask(task, mins)
                                            }
                                        }) {
                                            Text("Set")
                                        }
                                    }
                                }
                            }
                        } else {
                            // Regular Actions
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onCompleteTask(task) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Mark Complete", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { activeSnoozeTaskId = task.id },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Text("Snooze", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onStopTaskAlarm(task) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Alarm Screen - Dark Theme")
@Composable
fun AlarmScreenDarkPreview() {
    SoftTodoTheme(themeMode = "dark", colorSchemeType = "minimal") {
        AlarmScreen(
            activeTasks = listOf(
                Task(id = 1, title = "Critical Presentation Draft", description = "Review the slides for the meeting and send to manager", priority = 1, dateAdded = "2026-06-27"),
                Task(id = 2, title = "Call dentist", description = "Schedule checkup", priority = 3, dateAdded = "2026-06-27")
            ),
            onCompleteTask = {},
            onSnoozeTask = { _, _ -> },
            onStopTaskAlarm = {},
            onBackFallback = {}
        )
    }
}

@Preview(showBackground = true, name = "Alarm Screen - Light Theme")
@Composable
fun AlarmScreenLightPreview() {
    SoftTodoTheme(themeMode = "light", colorSchemeType = "minimal") {
        AlarmScreen(
            activeTasks = listOf(
                Task(id = 1, title = "Critical Presentation Draft", description = "Review the slides for the meeting and send to manager", priority = 1, dateAdded = "2026-06-27")
            ),
            onCompleteTask = {},
            onSnoozeTask = { _, _ -> },
            onStopTaskAlarm = {},
            onBackFallback = {}
        )
    }
}
