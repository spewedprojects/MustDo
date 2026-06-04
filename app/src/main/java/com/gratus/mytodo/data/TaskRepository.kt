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

    suspend fun insertTasks(tasks: List<Task>) = taskDao.insertTasks(tasks)
}
