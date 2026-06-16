package com.gratus.mytodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val nextReminderTime: Long? = null // Millisecond timestamp for the next repeating alarm, or null
)

/**
 * Data class representing a copy of a Task's key fields for the copy-paste action.
 */
data class CopiedTask(
    val title: String,
    val description: String,
    val priority: Int = 1,
    val reminderTime: Long? = null,
    val repeatCount: Int = 1
)

