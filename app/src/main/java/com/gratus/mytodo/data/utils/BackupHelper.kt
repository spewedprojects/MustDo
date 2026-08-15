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

package com.gratus.mytodo.data.utils

import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.SubTask
import org.json.JSONArray
import org.json.JSONObject

/**
 * Utility helper to handle JSON backup serialization (export) and deserialization (import).
 */
object BackupHelper {
    
    /**
     * Serializes a list of tasks into a formatted JSON string.
     */
    fun exportTasksToJson(tasks: List<Task>): String {
        val arr = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("priority", task.priority)
                put("dateAdded", task.dateAdded)
                put("isCompleted", task.isCompleted)
                put("reminderTime", task.reminderTime ?: JSONObject.NULL)
                put("isRecurring", task.isRecurring)
                put("createdSeq", task.createdSeq)
                put("repeatCount", task.repeatCount)
                put("repeatedTimes", task.repeatedTimes)
                put("isReminderActive", task.isReminderActive)
                put("nextReminderTime", task.nextReminderTime ?: JSONObject.NULL)
                put("category", task.category ?: JSONObject.NULL)
                put("reminderType", task.reminderType)
                put("snoozedUntil", task.snoozedUntil ?: JSONObject.NULL)
                put("deadlineDate", task.deadlineDate ?: JSONObject.NULL)
                put("terminatedDate", task.terminatedDate ?: JSONObject.NULL)
                put("subTasks", JSONArray().apply {
                    task.subTasks.forEach { sub ->
                        put(JSONObject().apply {
                            put("title", sub.title)
                            put("isCompleted", sub.isCompleted)
                        })
                    }
                })
            }
            arr.put(obj)
        }
        return arr.toString(2)
    }

    /**
     * Deserializes a JSON string into a list of tasks.
     * Safely migrates older backups (handling missing fields gracefully).
     */
    fun importTasksFromJson(jsonStr: String): List<Task> {
        val arr = JSONArray(jsonStr)
        val tasks = ArrayList<Task>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val reminderTime = if (obj.isNull("reminderTime")) null else obj.optLong("reminderTime", 0L).takeIf { it > 0L }
            val nextReminderTime = if (obj.has("nextReminderTime") && !obj.isNull("nextReminderTime")) {
                obj.optLong("nextReminderTime", 0L).takeIf { it > 0L }
            } else null
            
            val category = if (obj.has("category") && !obj.isNull("category")) obj.optString("category").takeIf { it.isNotBlank() } else null
            val reminderType = if (obj.has("reminderType") && !obj.isNull("reminderType")) obj.optString("reminderType", "notification") else "notification"
            val snoozedUntil = if (obj.has("snoozedUntil") && !obj.isNull("snoozedUntil")) {
                obj.optLong("snoozedUntil", 0L).takeIf { it > 0L }
            } else null

            val deadlineDate = if (obj.has("deadlineDate") && !obj.isNull("deadlineDate")) {
                obj.optString("deadlineDate").takeIf { it.isNotBlank() && it != "null" }
            } else null

            val terminatedDate = if (obj.has("terminatedDate") && !obj.isNull("terminatedDate")) {
                obj.optString("terminatedDate").takeIf { it.isNotBlank() && it != "null" }
            } else null

            val subTasksList = ArrayList<SubTask>()
            if (obj.has("subTasks") && !obj.isNull("subTasks")) {
                val subArr = obj.getJSONArray("subTasks")
                for (j in 0 until subArr.length()) {
                    val subObj = subArr.getJSONObject(j)
                    subTasksList.add(SubTask(
                        title = subObj.optString("title", ""),
                        isCompleted = if (subObj.has("isCompleted")) subObj.optBoolean("isCompleted", false) else false
                    ))
                }
            }

            val task = Task(
                id = if (obj.has("id")) obj.optInt("id", 0) else 0,
                title = obj.optString("title", ""),
                description = obj.optString("description", ""),
                priority = obj.optInt("priority", 1),
                dateAdded = obj.optString("dateAdded", ""),
                isCompleted = obj.optBoolean("isCompleted", false),
                reminderTime = reminderTime,
                isRecurring = if (obj.has("isRecurring")) obj.optBoolean("isRecurring", false) else false,
                createdSeq = if (obj.has("createdSeq")) obj.optLong("createdSeq", System.currentTimeMillis()) else System.currentTimeMillis(),
                repeatCount = if (obj.has("repeatCount")) obj.optInt("repeatCount", 1) else 1,
                repeatedTimes = if (obj.has("repeatedTimes")) obj.optInt("repeatedTimes", 0) else 0,
                isReminderActive = if (obj.has("isReminderActive")) obj.optBoolean("isReminderActive", true) else true,
                nextReminderTime = nextReminderTime ?: reminderTime,
                subTasks = subTasksList,
                category = category,
                reminderType = reminderType,
                snoozedUntil = snoozedUntil,
                deadlineDate = deadlineDate,
                terminatedDate = terminatedDate
            )
            tasks.add(task)
        }
        return tasks
    }
}
