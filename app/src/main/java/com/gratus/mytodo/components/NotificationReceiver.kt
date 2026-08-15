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

package com.gratus.mytodo.components

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gratus.mytodo.MainActivity
import com.gratus.mytodo.SnoozeActivity
import com.gratus.mytodo.AlarmActivity
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.TaskDatabase
import com.gratus.mytodo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for catching exact alarm triggers and displaying
 * user-facing reminder notifications.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = intent.getIntExtra("task_id", -1)
        if (taskId == -1) return

        if (action == ACTION_ALARM_TIMEOUT) {
            val pendingResult = goAsync()
            performSnooze(context, taskId, 5) {
                pendingResult.finish()
            }
            return
        }

        if (action == ACTION_MARK_COMPLETE) {
            val pendingResult = goAsync()
            val db = TaskDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null) {
                        val updated = task.copy(isCompleted = true, snoozedUntil = null)
                        db.taskDao().updateTask(updated)
                        cancelReminder(context, updated)
                        cancelAlarmTimeout(context, taskId)
                        
                        // Dismiss notification
                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.cancel(taskId)

                        // If it's an alarm, stop the AlarmService alarm
                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            this.action = AlarmService.ACTION_STOP_ALARM
                            putExtra("task_id", taskId)
                        }
                        context.startService(serviceIntent)
                        
                        // Notify widgets
                        val updateIntent = Intent("com.gratus.mytodo.action.WIDGET_UPDATE").apply {
                            setPackage(context.packageName)
                        }
                        context.sendBroadcast(updateIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationReceiver", "Error marking complete: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == ACTION_STOP_REMINDERS) {
            val pendingResult = goAsync()
            val db = TaskDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null) {
                        val updated = task.copy(isReminderActive = false, repeatedTimes = 0, nextReminderTime = null, snoozedUntil = null)
                        db.taskDao().updateTask(updated)
                        cancelReminder(context, task)
                        cancelAlarmTimeout(context, taskId)
                        
                        // Dismiss notification
                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.cancel(taskId)

                        // If it's an alarm, stop the AlarmService alarm
                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            this.action = AlarmService.ACTION_STOP_ALARM
                            putExtra("task_id", taskId)
                        }
                        context.startService(serviceIntent)
                        
                        // Notify widgets
                        val updateIntent = Intent("com.gratus.mytodo.action.WIDGET_UPDATE").apply {
                            setPackage(context.packageName)
                        }
                        context.sendBroadcast(updateIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationReceiver", "Error stopping reminders: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            val pendingResult = goAsync()
            val db = TaskDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val task = db.taskDao().getTaskById(taskId)
                    val sharedPrefs = context.getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)
                    val isStickyEnabled = sharedPrefs.getBoolean("enable_sticky_tasks", true)
                    val isStickyTask = task?.category?.equals("Sticky", ignoreCase = true) == true
                    
                    if (task != null && !task.isCompleted && task.isReminderActive && !(isStickyTask && !isStickyEnabled)) {
                        if (task.reminderType == "alarm") {
                            // Start Alarm Service
                            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                                this.action = AlarmService.ACTION_START_ALARM
                                putExtra("task_id", taskId)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                            // Schedule safety timeout alarm
                            scheduleAlarmTimeout(context, taskId)
                        } else {
                            // Regular Notification reminder
                            showNotification(context, task)
                        }

                        val todayStr = com.gratus.mytodo.ui.utils.DateTimeUtils.formatDbDate(System.currentTimeMillis())
                        val isStickyTask = task.category?.equals("Sticky", ignoreCase = true) == true

                        // If it's a sticky task and this task entry is not for today, operate on/create today's entry
                        val targetTask = if (isStickyTask && task.dateAdded != todayStr) {
                            val directToday = db.taskDao().getTasksForDateDirect(todayStr).find {
                                it.category?.equals("Sticky", ignoreCase = true) == true &&
                                it.title.trim().equals(task.title.trim(), ignoreCase = true)
                            }
                            if (directToday != null) {
                                directToday
                            } else {
                                val newToday = task.copy(
                                    id = 0,
                                    dateAdded = todayStr,
                                    repeatedTimes = 0,
                                    snoozedUntil = null
                                )
                                val newId = db.taskDao().insertTask(newToday).toInt()
                                newToday.copy(id = newId)
                            }
                        } else {
                            task
                        }

                        // Clear snooze field in DB since the alarm/reminder fired now
                        val updatedTaskWithReset = targetTask.copy(snoozedUntil = null)
                        db.taskDao().updateTask(updatedTaskWithReset)

                        // Handle repeating alerts: 1x to 4x
                        if (targetTask.repeatedTimes + 1 < targetTask.repeatCount) {
                            val sharedPrefs = context.getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)
                            val nextAlarmTime = System.currentTimeMillis() + (sharedPrefs.getInt("reminder_repeat_interval", 10) * 60 * 1000L)
                            
                            val updatedTask = updatedTaskWithReset.copy(
                                repeatedTimes = targetTask.repeatedTimes + 1,
                                nextReminderTime = nextAlarmTime
                            )
                            db.taskDao().updateTask(updatedTask)
                            scheduleExactReminder(context, updatedTask)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(context: Context, task: Task) {
        val channelId = "todo_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MustDo Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent reminder notifications for scheduled tasks."
            }
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            task.id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Complete action
        val markCompleteIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_MARK_COMPLETE
            putExtra("task_id", task.id)
        }
        val markCompletePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id + 100000,
            markCompleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action
        val stopIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_STOP_REMINDERS
            putExtra("task_id", task.id)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            task.id + 200000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(context, SnoozeActivity::class.java).apply {
            putExtra("task_id", task.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val snoozePendingIntent = PendingIntent.getActivity(
            context,
            task.id + 300000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityText = when (task.priority) {
            1 -> "Priority 1 (Urgent)"
            2 -> "Priority 2 (High)"
            3 -> "Priority 3 (Medium)"
            4 -> "Priority 4 (Low)"
            else -> "Priority ${task.priority}"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.icon_v3_notif)
            .setContentTitle("Reminder: ${task.title}")
            .setContentText(priorityText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Mark Complete", markCompletePendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .addAction(0, "Snooze", snoozePendingIntent)
            .build()

        manager.notify(task.id, notification)
    }

    companion object {
        const val ACTION_MARK_COMPLETE = "com.gratus.mytodo.action.MARK_COMPLETE"
        const val ACTION_STOP_REMINDERS = "com.gratus.mytodo.action.STOP_REMINDERS"
        const val ACTION_ALARM_TIMEOUT = "com.gratus.mytodo.action.ALARM_TIMEOUT"

        fun rescheduleAllAlarms(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val sharedPrefs = context.getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)
                    val isStickyEnabled = sharedPrefs.getBoolean("enable_sticky_tasks", true)
                    val db = TaskDatabase.getDatabase(context)
                    val tasks = db.taskDao().getAllTasksDirect()
                    val currentTime = System.currentTimeMillis()
                    for (task in tasks) {
                        val isStickyTask = task.category?.equals("Sticky", ignoreCase = true) == true
                        val time = task.nextReminderTime ?: task.reminderTime
                        if (task.isCompleted || !task.isReminderActive || time == null || time <= currentTime || (isStickyTask && !isStickyEnabled)) {
                            cancelReminder(context, task)
                        } else {
                            scheduleExactReminder(context, task)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationReceiver", "Failed to reschedule alarms", e)
                }
            }
        }

        fun scheduleExactReminder(context: Context, task: Task) {
            val sharedPrefs = context.getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)
            val isStickyEnabled = sharedPrefs.getBoolean("enable_sticky_tasks", true)
            val isStickyTask = task.category?.equals("Sticky", ignoreCase = true) == true
            if (isStickyTask && !isStickyEnabled) return

            val time = task.nextReminderTime ?: task.reminderTime ?: return
            if (!task.isReminderActive || task.isCompleted) return
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("task_id", task.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (task.reminderType == "alarm") {
                    val openIntent = Intent(context, AlarmActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    val showIntent = PendingIntent.getActivity(
                        context,
                        task.id + 400000,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(time, showIntent), pendingIntent)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                    }
                }
            } catch (e: SecurityException) {
                android.util.Log.e("NotificationReceiver", "Permission denied for exact alarms: ${e.message}")
            }
        }

        fun cancelReminder(context: Context, task: Task) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            cancelAlarmTimeout(context, task.id)
        }

        fun scheduleAlarmTimeout(context: Context, taskId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_ALARM_TIMEOUT
                putExtra("task_id", taskId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + 500000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + (2 * 60 * 1000L) // 2 minutes
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (e: SecurityException) {
                android.util.Log.e("NotificationReceiver", "Permission denied scheduling timeout: ${e.message}")
            }
        }

        fun cancelAlarmTimeout(context: Context, taskId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_ALARM_TIMEOUT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId + 500000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        fun performSnooze(context: Context, taskId: Int, snoozeMins: Int, onComplete: (() -> Unit)? = null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = TaskDatabase.getDatabase(context)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null) {
                        cancelReminder(context, task)
                        cancelAlarmTimeout(context, taskId)

                        val prevRepeatedTimes = (task.repeatedTimes - 1).coerceAtLeast(0)
                        val snoozeUntil = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)

                        val updatedTask = task.copy(
                            repeatedTimes = prevRepeatedTimes,
                            nextReminderTime = snoozeUntil,
                            snoozedUntil = snoozeUntil
                        )
                        db.taskDao().updateTask(updatedTask)

                        scheduleExactReminder(context, updatedTask)

                        // Stop service alarm for this task
                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            action = AlarmService.ACTION_STOP_ALARM
                            putExtra("task_id", taskId)
                        }
                        context.startService(serviceIntent)

                        // Dismiss notification
                        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.cancel(taskId)

                        // Notify widgets
                        val updateIntent = Intent("com.gratus.mytodo.action.WIDGET_UPDATE").apply {
                            setPackage(context.packageName)
                        }
                        context.sendBroadcast(updateIntent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationReceiver", "Error performSnooze: ${e.message}", e)
                } finally {
                    onComplete?.invoke()
                }
            }
        }
    }
}
