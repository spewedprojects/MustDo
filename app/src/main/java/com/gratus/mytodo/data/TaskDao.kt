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

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the tasks table, defining reactive Flow queries and suspend functions.
 */
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE dateAdded = :date ORDER BY createdSeq ASC")
    fun getTasksForDateFlow(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY dateAdded DESC, createdSeq ASC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR dateAdded LIKE '%' || :query || '%' ORDER BY dateAdded DESC")
    fun searchTasksFlow(query: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT priority FROM tasks ORDER BY createdSeq DESC LIMIT 1")
    suspend fun getLastUsedPriority(): Int?

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksDirect(): List<Task>

    @Query("SELECT * FROM tasks WHERE dateAdded = :date ORDER BY createdSeq ASC")
    suspend fun getTasksForDateDirect(date: String): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Query("UPDATE tasks SET category = NULL WHERE category = :category")
    suspend fun removeCategoryFromTasks(category: String)
}
