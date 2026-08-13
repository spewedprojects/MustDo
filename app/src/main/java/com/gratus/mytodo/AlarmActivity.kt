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

import com.gratus.mytodo.ui.components.alarm.AlarmScreen

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

        enableEdgeToEdge()
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
