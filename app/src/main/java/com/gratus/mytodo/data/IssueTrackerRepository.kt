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

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class IssueTrackerRepository(private val context: Context) {
    private val fileName = "issues.json"
    private val file: File
        get() = File(context.filesDir, fileName)

    suspend fun getIssues(): List<IssueItem> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        
        try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<IssueItem>()
            for (i in 0 until jsonArray.length()) {
                list.add(IssueItem.fromJson(jsonArray.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveIssues(issues: List<IssueItem>) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        issues.forEach { jsonArray.put(it.toJson()) }
        // Pretty print with 4 spaces indent
        file.writeText(jsonArray.toString(4))
    }
}
