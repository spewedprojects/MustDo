package com.gratus.mytodo.components

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gratus.mytodo.MainActivity
import com.gratus.mytodo.data.TaskDatabase
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

        // Fetch task details in background thread
        val db = TaskDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val task = db.taskDao().getTaskById(taskId) ?: return@launch
            
            // Show notification if task is not completed yet
            if (!task.isCompleted) {
                showNotification(context, task.id, task.title, task.priority, task.description)
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, priority: Int, desc: String) {
        val channelId = "todo_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "To-Do Task Reminders",
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
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Reminder: $title")
            .setContentText("$priorityText - $desc")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }
}
