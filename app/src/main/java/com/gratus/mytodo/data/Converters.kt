package com.gratus.mytodo.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Type converters for Room to serialize and deserialize custom objects using Moshi.
 */
class Converters {
    private val moshi = Moshi.Builder().build()
    private val type = Types.newParameterizedType(List::class.java, SubTask::class.java)
    private val adapter = moshi.adapter<List<SubTask>>(type)

    @TypeConverter
    fun fromSubTaskList(value: List<SubTask>?): String {
        return adapter.toJson(value ?: emptyList())
    }

    @TypeConverter
    fun toSubTaskList(value: String?): List<SubTask> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
