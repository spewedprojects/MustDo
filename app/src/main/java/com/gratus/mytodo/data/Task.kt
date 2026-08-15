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

package com.gratus.mytodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Data class representing a sub-task item.
 */
@JsonClass(generateAdapter = true)
data class SubTask(
    val title: String,
    val isCompleted: Boolean = false
)

/**
 * Entity representing a To-Do Task in the Room database.
 * 
 * Each task belongs strictly to the date it was added (captured by [dateAdded]).
 * If left incomplete after the whole day, it is marked as incomplete.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val priority: Int = 1, // Priority levels from 1 (Red/Urgent) to 4 (Yellow/Low)
    val dateAdded: String, // String representation format "yyyy-MM-dd"
    val isCompleted: Boolean = false,
    val reminderTime: Long? = null, // Millisecond timestamp for the notification alarm, or null
    val isRecurring: Boolean = false, // If true, replicates to future dates or adds automatically
    val createdSeq: Long = System.currentTimeMillis(), // Sequence representing addition time/sequence
    val repeatCount: Int = 1, // Repeating count from 1x to 4x (default 1)
    val repeatedTimes: Int = 0, // Number of repeating alerts completed
    val isReminderActive: Boolean = true, // If false, the reminder alerts are suspended for the task
    val nextReminderTime: Long? = null, // Millisecond timestamp for the next repeating alarm, or null
    val subTasks: List<SubTask> = emptyList(), // JSON serialized list of sub-tasks
    val category: String? = null, // Nullable category tag (e.g. Work, Personal)
    val reminderType: String = "notification", // "notification" or "alarm"
    val snoozedUntil: Long? = null, // Millisecond timestamp for when the snooze expires, or null
    val deadlineDate: String? = null, // Last date or deadline string "yyyy-MM-dd" for sticky tasks, or null
    val terminatedDate: String? = null // Date on which the sticky task was terminated, or null
)

/**
 * Data class representing a copy of a Task's key fields for the copy-paste action.
 */
data class CopiedTask(
    val title: String,
    val description: String,
    val priority: Int = 1,
    val reminderTime: Long? = null,
    val repeatCount: Int = 1,
    val subTasks: List<SubTask> = emptyList(),
    val category: String? = null,
    val reminderType: String = "notification",
    val originalDateAdded: String = ""
)
