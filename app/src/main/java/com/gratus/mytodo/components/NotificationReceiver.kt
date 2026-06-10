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
        val taskId = intent.getIntExtra("task_id", -1)
        if (taskId == -1) return

        val pendingResult = goAsync()
        // Fetch task details in background thread
        val db = TaskDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = db.taskDao().getTaskById(taskId)
                
                // Show notification if task is not completed yet
                if (task != null && !task.isCompleted) {
                    showNotification(context, task.id, task.title, task.priority, task.description)

                    // Handle repeating alerts: 1x to 4x
                    if (task.repeatedTimes + 1 < task.repeatCount) {
                        val sharedPrefs = context.getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)
                        val intervalMins = sharedPrefs.getInt("reminder_repeat_interval", 10)
                        val nextAlarmTime = System.currentTimeMillis() + (intervalMins * 60 * 1000L)
                        
                        val updatedTask = task.copy(
                            repeatedTimes = task.repeatedTimes + 1,
                            reminderTime = nextAlarmTime
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

    private fun showNotification(context: Context, id: Int, title: String, priority: Int, desc: String) {
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
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityText = when (priority) {
            1 -> "Priority 1 (Urgent)"
            2 -> "Priority 2 (High)"
            3 -> "Priority 3 (Medium)"
            4 -> "Priority 4 (Low)"
            else -> "Priority $priority"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.icon_v3)
            .setContentTitle("Reminder: $title")
            .setContentText("$priorityText - $desc")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }

    companion object {
        fun scheduleExactReminder(context: Context, task: Task) {
            val time = task.reminderTime ?: return
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
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
        }
    }
}
