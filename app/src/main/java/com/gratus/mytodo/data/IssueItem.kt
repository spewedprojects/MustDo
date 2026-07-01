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

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class IssueItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String,
    val isClosed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val comments: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("title", title)
        json.put("description", description)
        json.put("category", category)
        json.put("isClosed", isClosed)
        json.put("timestamp", timestamp)
        
        val commentsArray = JSONArray()
        comments.forEach { commentsArray.put(it) }
        json.put("comments", commentsArray)
        
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): IssueItem {
            val commentsArray = json.optJSONArray("comments")
            val commentsList = mutableListOf<String>()
            if (commentsArray != null) {
                for (i in 0 until commentsArray.length()) {
                    commentsList.add(commentsArray.getString(i))
                }
            }
            
            return IssueItem(
                id = json.getString("id"),
                title = json.getString("title"),
                description = json.getString("description"),
                category = json.getString("category"),
                isClosed = json.getBoolean("isClosed"),
                timestamp = json.getLong("timestamp"),
                comments = commentsList
            )
        }
    }
}
