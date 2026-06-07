package com.gratus.mytodo.data.utils

import com.gratus.mytodo.data.Task
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
            }
            arr.put(obj)
        }
        return arr.toString(2)
    }

    /**
     * Deserializes a JSON string into a list of tasks.
     */
    fun importTasksFromJson(jsonStr: String): List<Task> {
        val arr = JSONArray(jsonStr)
        val tasks = ArrayList<Task>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val reminderTime = if (obj.isNull("reminderTime")) null else obj.getLong("reminderTime")
            val task = Task(
                id = if (obj.has("id")) obj.getInt("id") else 0,
                title = obj.getString("title"),
                description = obj.getString("description"),
                priority = obj.getInt("priority"),
                dateAdded = obj.getString("dateAdded"),
                isCompleted = obj.getBoolean("isCompleted"),
                reminderTime = reminderTime,
                isRecurring = if (obj.has("isRecurring")) obj.getBoolean("isRecurring") else false,
                createdSeq = if (obj.has("createdSeq")) obj.getLong("createdSeq") else System.currentTimeMillis()
            )
            tasks.add(task)
        }
        return tasks
    }
}
