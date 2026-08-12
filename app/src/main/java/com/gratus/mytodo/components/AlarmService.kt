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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import com.gratus.mytodo.AlarmActivity
import com.gratus.mytodo.R
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AlarmState {
    val activeTasks = mutableStateListOf<Task>()
}

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val action = intent.action
        val taskId = intent.getIntExtra("task_id", -1)

        when (action) {
            ACTION_START_ALARM -> {
                if (taskId != -1) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = TaskDatabase.getDatabase(applicationContext)
                        val task = db.taskDao().getTaskById(taskId)
                        if (task != null && !task.isCompleted && task.isReminderActive) {
                            withContext(Dispatchers.Main) {
                                if (!AlarmState.activeTasks.any { it.id == taskId }) {
                                    AlarmState.activeTasks.add(task)
                                }
                                startAlarmSound()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    startForeground(
                                        NOTIFICATION_ID,
                                        buildNotification(AlarmState.activeTasks),
                                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                                    )
                                } else {
                                    startForeground(NOTIFICATION_ID, buildNotification(AlarmState.activeTasks))
                                }
                                
                                // Launch AlarmActivity
                                val activityIntent = Intent(this@AlarmService, AlarmActivity::class.java).apply {
                                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(activityIntent)
                            }
                        }
                    }
                }
            }
            ACTION_STOP_ALARM -> {
                if (taskId != -1) {
                    val index = AlarmState.activeTasks.indexOfFirst { it.id == taskId }
                    if (index != -1) {
                        AlarmState.activeTasks.removeAt(index)
                    }
                    updateNotification()
                }
            }
            ACTION_STOP_ALL_ALARMS -> {
                AlarmState.activeTasks.clear()
                stopAlarmSound()
                stopForeground(true)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startAlarmSound() {
        if (mediaPlayer != null) return // Already playing
        try {
            val sharedPrefs = getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)
            val ringtoneUriStr = sharedPrefs.getString("alarm_ringtone_uri", null)
            val uri = ringtoneUriStr?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("AlarmService", "Failed to play preferred alarm tone: ${e.message}", e)
            try {
                // Fallback to default alarm
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@AlarmService, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                android.util.Log.e("AlarmService", "Failed to play default alarm tone: ${e2.message}", e2)
            }
        }
    }

    private fun stopAlarmSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            android.util.Log.e("AlarmService", "Error stopping MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (AlarmState.activeTasks.isEmpty()) {
            stopAlarmSound()
            stopForeground(true)
            stopSelf()
        } else {
            manager.notify(NOTIFICATION_ID, buildNotification(AlarmState.activeTasks))
        }
    }

    private fun buildNotification(tasks: List<Task>): android.app.Notification {
        val openIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            9999,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            9998,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopAllIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALL_ALARMS
        }
        val stopAllPendingIntent = PendingIntent.getService(
            this,
            9997,
            stopAllIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (tasks.size == 1) {
            "Alarm: ${tasks[0].title}"
        } else {
            "${tasks.size} Active Alarms"
        }

        val bodyText = if (tasks.size == 1) {
            tasks[0].description.ifBlank { "Priority ${tasks[0].priority} Alarm" }
        } else {
            tasks.joinToString(", ") { it.title }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_v3_notif)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setDeleteIntent(stopAllPendingIntent)
            .addAction(R.drawable.icon_v3_notif, "Stop Alarm", stopAllPendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MustDo Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alarm alerts for MustDo tasks"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setSound(null, null)
                enableVibration(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setBypassDnd(true)
                }
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAlarmSound()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "alarm_reminders"
        const val NOTIFICATION_ID = 8888
        const val ACTION_START_ALARM = "com.gratus.mytodo.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.gratus.mytodo.action.STOP_ALARM"
        const val ACTION_STOP_ALL_ALARMS = "com.gratus.mytodo.action.STOP_ALL_ALARMS"
    }
}
