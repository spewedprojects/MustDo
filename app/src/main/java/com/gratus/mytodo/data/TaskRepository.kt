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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository layer to abstract database transactions from ViewModels.
 */
class TaskRepository(private val taskDao: TaskDao) {

    fun getTasksForDate(date: String): Flow<List<Task>> = taskDao.getTasksForDateFlow(date)

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasksFlow()

    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasksFlow(query)

    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun getLastUsedPriority(): Int {
        return taskDao.getLastUsedPriority() ?: 1
    }

    suspend fun getAllTasksDirect(): List<Task> = taskDao.getAllTasksDirect()

    suspend fun getTasksForDateDirect(date: String): List<Task> = taskDao.getTasksForDateDirect(date)

    suspend fun insertTasks(tasks: List<Task>) = taskDao.insertTasks(tasks)

    suspend fun removeCategoryFromTasks(category: String) = taskDao.removeCategoryFromTasks(category)
}
