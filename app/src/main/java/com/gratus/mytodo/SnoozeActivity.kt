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

import com.gratus.mytodo.ui.components.alarm.SnoozeDialog

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
        val colorfulHueShift = sharedPrefs.getFloat("colorful_hue_shift", 0f)
        val colorfulSatScale = sharedPrefs.getFloat("colorful_sat_scale", 1f)

        setContent {
            SoftTodoTheme(
                themeMode = themeMode,
                colorSchemeType = colorSchemeType,
                colorfulHueShift = colorfulHueShift,
                colorfulSatScale = colorfulSatScale
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
