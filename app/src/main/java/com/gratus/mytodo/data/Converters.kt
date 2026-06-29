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
