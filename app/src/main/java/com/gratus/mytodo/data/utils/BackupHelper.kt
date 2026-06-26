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
     */
    fun importTasksFromJson(jsonStr: String): List<Task> {
        val arr = JSONArray(jsonStr)
        val tasks = ArrayList<Task>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val reminderTime = if (obj.isNull("reminderTime")) null else obj.getLong("reminderTime")
            val nextReminderTime = if (obj.has("nextReminderTime") && !obj.isNull("nextReminderTime")) obj.getLong("nextReminderTime") else null
            
            val category = if (obj.has("category") && !obj.isNull("category")) obj.getString("category") else null
            val reminderType = if (obj.has("reminderType") && !obj.isNull("reminderType")) obj.getString("reminderType") else "notification"
            val snoozedUntil = if (obj.has("snoozedUntil") && !obj.isNull("snoozedUntil")) obj.getLong("snoozedUntil") else null

            val subTasksList = ArrayList<SubTask>()
            if (obj.has("subTasks")) {
                val subArr = obj.getJSONArray("subTasks")
                for (j in 0 until subArr.length()) {
                    val subObj = subArr.getJSONObject(j)
                    subTasksList.add(SubTask(
                        title = subObj.getString("title"),
                        isCompleted = if (subObj.has("isCompleted")) subObj.getBoolean("isCompleted") else false
                    ))
                }
            }

            val task = Task(
                id = if (obj.has("id")) obj.getInt("id") else 0,
                title = obj.getString("title"),
                description = obj.getString("description"),
                priority = obj.getInt("priority"),
                dateAdded = obj.getString("dateAdded"),
                isCompleted = obj.getBoolean("isCompleted"),
                reminderTime = reminderTime,
                isRecurring = if (obj.has("isRecurring")) obj.getBoolean("isRecurring") else false,
                createdSeq = if (obj.has("createdSeq")) obj.getLong("createdSeq") else System.currentTimeMillis(),
                repeatCount = if (obj.has("repeatCount")) obj.getInt("repeatCount") else 1,
                repeatedTimes = if (obj.has("repeatedTimes")) obj.getInt("repeatedTimes") else 0,
                isReminderActive = if (obj.has("isReminderActive")) obj.getBoolean("isReminderActive") else true,
                nextReminderTime = nextReminderTime ?: reminderTime,
                subTasks = subTasksList,
                category = category,
                reminderType = reminderType,
                snoozedUntil = snoozedUntil
            )
            tasks.add(task)
        }
        return tasks
    }
}
