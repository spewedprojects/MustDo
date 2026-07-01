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

package com.gratus.mytodo.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gratus.mytodo.components.NotificationReceiver
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.CopiedTask
import com.gratus.mytodo.data.SubTask
import com.gratus.mytodo.data.TaskDatabase
import com.gratus.mytodo.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.gratus.mytodo.data.utils.BackupHelper
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.*

/**
 * Navigation screen selection.
 */
enum class Screen {
    HOME,
    HISTORY,
    STATS,
    SETTINGS,
    ISSUE_TRACKER
}

/**
 * Task sorting modes.
 */
enum class SortOption {
    PRIORITY,
    ADDED_SEQUENCE
}

/**
 * Historical filter modes.
 */
enum class FilterOption {
    ALL,
    MARKED_COMPLETE,
    LEFT_INCOMPLETE
}

/**
 * Core ViewModel designed in MVVM pattern, strictly separating logic from Compose UI.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    private val sharedPrefs = application.getSharedPreferences("soft_todo_prefs", Context.MODE_PRIVATE)

    // Current screen navigation state
    private val _activeScreen = MutableStateFlow(Screen.HOME)
    val activeScreen: StateFlow<Screen> = _activeScreen.asStateFlow()

    // Current focus date for main screen
    private val _currentDate = MutableStateFlow(Calendar.getInstance())
    val currentDate: StateFlow<Calendar> = _currentDate.asStateFlow()

    // Tasks list for the active date on home screen
    private val _sortingOption = MutableStateFlow(
        SortOption.valueOf(sharedPrefs.getString("sort_option", SortOption.ADDED_SEQUENCE.name) ?: SortOption.ADDED_SEQUENCE.name)
    )
    val sortingOption: StateFlow<SortOption> = _sortingOption.asStateFlow()

    // Settings States
    private val _settingsTheme = MutableStateFlow(sharedPrefs.getString("theme", "auto") ?: "auto")
    val settingsTheme: StateFlow<String> = _settingsTheme.asStateFlow()

    private val _settingsColorScheme = MutableStateFlow(sharedPrefs.getString("color_scheme", "minimal") ?: "minimal")
    val settingsColorScheme: StateFlow<String> = _settingsColorScheme.asStateFlow()

    private val _lastUsedPriority = MutableStateFlow(sharedPrefs.getInt("last_priority", 1))
    val lastUsedPriority: StateFlow<Int> = _lastUsedPriority.asStateFlow()

    private val _settingsReminderInterval = MutableStateFlow(sharedPrefs.getInt("reminder_repeat_interval", 10))
    val settingsReminderInterval: StateFlow<Int> = _settingsReminderInterval.asStateFlow()

    fun setReminderInterval(minutes: Int) {
        _settingsReminderInterval.value = minutes
        sharedPrefs.edit().putInt("reminder_repeat_interval", minutes).apply()
    }

    private val _settingsAlarmRingtone = MutableStateFlow<String?>(sharedPrefs.getString("alarm_ringtone_uri", null))
    val settingsAlarmRingtone: StateFlow<String?> = _settingsAlarmRingtone.asStateFlow()

    fun setAlarmRingtone(uriString: String?) {
        _settingsAlarmRingtone.value = uriString
        if (uriString != null) {
            sharedPrefs.edit().putString("alarm_ringtone_uri", uriString).apply()
        } else {
            sharedPrefs.edit().remove("alarm_ringtone_uri").apply()
        }
    }

    // Category / Tag Management
    private val defaultCategories = listOf("Personal", "Work", "Errands", "Health", "Learning")
    private val _customCategories = MutableStateFlow<List<String>>(
        sharedPrefs.getStringSet("custom_categories", emptySet())?.toList()?.sorted() ?: emptyList()
    )
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    val categories: StateFlow<List<String>> = _customCategories
        .map { custom -> (defaultCategories + custom).distinct() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, defaultCategories)

    fun addCustomCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return
        val current = _customCategories.value.toMutableList()
        if (!current.contains(trimmed) && !defaultCategories.contains(trimmed)) {
            current.add(trimmed)
            current.sort()
            _customCategories.value = current
            sharedPrefs.edit().putStringSet("custom_categories", current.toSet()).apply()
        }
    }

    fun deleteCustomCategory(category: String) {
        val current = _customCategories.value.toMutableList()
        if (current.remove(category)) {
            _customCategories.value = current
            sharedPrefs.edit().putStringSet("custom_categories", current.toSet()).apply()
            
            // Clean up DB references
            viewModelScope.launch {
                repository.removeCategoryFromTasks(category)
                updateWidget()
            }
        }
    }

    private val _isAlarmPermissionGranted = MutableStateFlow(true)
    val isAlarmPermissionGranted: StateFlow<Boolean> = _isAlarmPermissionGranted.asStateFlow()

    private val _isNotificationPermissionGranted = MutableStateFlow(true)
    val isNotificationPermissionGranted: StateFlow<Boolean> = _isNotificationPermissionGranted.asStateFlow()

    fun checkPermissions(context: Context) {
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _isNotificationPermissionGranted.value = notificationGranted

        val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (e: Exception) {
                false
            }
        } else {
            true
        }
        _isAlarmPermissionGranted.value = alarmGranted
    }

    // Dialog / Edit Screen States (preserved across screen rotations)
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _taskToEdit = MutableStateFlow<Task?>(null)
    val taskToEdit: StateFlow<Task?> = _taskToEdit.asStateFlow()

    private val _taskToDelete = MutableStateFlow<Task?>(null)
    val taskToDelete: StateFlow<Task?> = _taskToDelete.asStateFlow()

    private val _copiedTask = MutableStateFlow<CopiedTask?>(null)
    val copiedTask: StateFlow<CopiedTask?> = _copiedTask.asStateFlow()

    fun setCopiedTask(task: CopiedTask?) {
        _copiedTask.value = task
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun setTaskToEdit(task: Task?) {
        _taskToEdit.value = task
    }

    fun setTaskToDelete(task: Task?) {
        _taskToDelete.value = task
    }

    // Historical screen states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyZoomLevel = MutableStateFlow(3)
    val historyZoomLevel: StateFlow<Int> = _historyZoomLevel.asStateFlow()

    private val _historyFilter = MutableStateFlow(FilterOption.ALL)
    val historyFilter: StateFlow<FilterOption> = _historyFilter.asStateFlow()

    // Database Flows

    init {
        val database = TaskDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
        
        // Fetch last used priority on launch
        viewModelScope.launch {
            _lastUsedPriority.value = repository.getLastUsedPriority()
        }
        checkPermissions(application)
        updateWidget()
        
        // Reschedule alarms to keep in sync
        NotificationReceiver.rescheduleAllAlarms(application)
    }

    /**
     * Set active drawer screen.
     */
    fun setActiveScreen(screen: Screen) {
        _activeScreen.value = screen
    }

    /**
     * Swiping / navigating dates on Home Screen.
     */
    fun navigateDate(days: Int) {
        val newCal = Calendar.getInstance().apply {
            time = _currentDate.value.time
            add(Calendar.DAY_OF_YEAR, days)
        }
        _currentDate.value = newCal
    }

    fun setDate(calendar: Calendar) {
        _currentDate.value = calendar
    }

    /**
     * Get reactive task lists for current date on Home Screen.
     */
    val homeTasks: Flow<List<Task>> = _currentDate
        .map { cal -> DateTimeUtils.formatDbDate(cal) }
        .flatMapLatest { dateStr -> repository.getTasksForDate(dateStr) }
        .combine(_sortingOption) { taskList, sort ->
            when (sort) {
                SortOption.PRIORITY -> taskList.sortedBy { it.priority }
                SortOption.ADDED_SEQUENCE -> taskList.sortedBy { it.createdSeq }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Get tasks list flow for a specific date (used by the smooth sliding HorizontalPager).
     */
    fun getTasksForDateFlow(dateStr: String): Flow<List<Task>> {
        return repository.getTasksForDate(dateStr)
            .combine(_sortingOption) { taskList, sort ->
                when (sort) {
                    SortOption.PRIORITY -> taskList.sortedBy { it.priority }
                    SortOption.ADDED_SEQUENCE -> taskList.sortedBy { it.createdSeq }
                }
            }
    }

    /**
     * Reactive task lists for history screen (filters applied via queries or combination).
     */
    val historyTasks: Flow<List<Task>> = combine(
        _searchQuery,
        _historyFilter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        val baseFlow = if (query.isBlank()) {
            repository.getAllTasks()
        } else {
            repository.searchTasks(query)
        }
        
        baseFlow.map { list ->
            val todayStr = DateTimeUtils.formatDbDate(System.currentTimeMillis())
            val trimmedQuery = query.trim()
            val isDateQuery = trimmedQuery.matches(Regex("\\d{4}-\\d{2}(-\\d{2})?"))
            list.filter { it.dateAdded <= todayStr || (isDateQuery && it.dateAdded.startsWith(trimmedQuery)) }
                .filter { task ->
                    when (filter) {
                        FilterOption.ALL -> true
                        FilterOption.MARKED_COMPLETE -> task.isCompleted
                        FilterOption.LEFT_INCOMPLETE -> {
                            // Left incomplete: in the past and isCompleted is false
                            !task.isCompleted && task.dateAdded < todayStr
                        }
                    }
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Change main sorting.
     */
    fun toggleSorting() {
        val next = if (_sortingOption.value == SortOption.PRIORITY) {
            SortOption.ADDED_SEQUENCE
        } else {
            SortOption.PRIORITY
        }
        _sortingOption.value = next
        sharedPrefs.edit().putString("sort_option", next.name).apply()
    }

    /**
     * Task insertions from Dialog.
     */
    fun addTask(
        title: String,
        description: String,
        priority: Int,
        targetDate: Calendar,
        replicateDates: List<String>, // yyyy-MM-dd format future copies
        everydayDaysCount: Int = 0, // if everyday is checked, range of next everyday copies
        reminderTimeMillis: Long? = null,
        repeatCount: Int = 1,
        subTasks: List<SubTask> = emptyList(),
        category: String? = null,
        reminderType: String = "notification"
    ) {
        viewModelScope.launch {
            val dateStr = DateTimeUtils.formatDbDate(targetDate)
            val baseTask = Task(
                title = title,
                description = description,
                priority = priority,
                dateAdded = dateStr,
                reminderTime = reminderTimeMillis,
                isRecurring = everydayDaysCount > 0,
                createdSeq = System.currentTimeMillis(),
                repeatCount = repeatCount,
                repeatedTimes = 0,
                isReminderActive = true,
                nextReminderTime = reminderTimeMillis,
                subTasks = subTasks,
                category = category,
                reminderType = reminderType,
                snoozedUntil = null
            )

            // Save last used priority
            _lastUsedPriority.value = priority
            sharedPrefs.edit().putInt("last_priority", priority).apply()

            val baseId = repository.insertTask(baseTask).toInt()
            
            // Set alarm if custom reminder was scheduled
            if (reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
                val scheduledTask = baseTask.copy(id = baseId)
                scheduleExactReminder(scheduledTask)
            }

            // Replicate to custom selected future dates
            replicateDates.forEach { futureDate ->
                if (futureDate != dateStr) {
                    val futureTask = baseTask.copy(dateAdded = futureDate, isRecurring = false, reminderTime = null, nextReminderTime = null, reminderType = reminderType, snoozedUntil = null)
                    repository.insertTask(futureTask)
                }
            }

            // Replicate automatically to "everyday" range
            if (everydayDaysCount > 0) {
                for (i in 1..everydayDaysCount) {
                    val runCal = Calendar.getInstance().apply {
                        time = targetDate.time
                        add(Calendar.DAY_OF_YEAR, i)
                    }
                    val dailyStr = DateTimeUtils.formatDbDate(runCal)
                    val everydayTask = baseTask.copy(dateAdded = dailyStr, isRecurring = true, reminderTime = null, nextReminderTime = null, reminderType = reminderType, snoozedUntil = null)
                    repository.insertTask(everydayTask)
                }
            }

            updateWidget()
        }
    }

    /**
     * Complete task.
     */
    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            val updatedSubTasks = task.subTasks.map { it.copy(isCompleted = newCompleted) }
            val updated = task.copy(
                isCompleted = newCompleted,
                subTasks = updatedSubTasks,
                snoozedUntil = null
            )
            val updatedWithReset = if (!updated.isCompleted) {
                updated.copy(repeatedTimes = 0, isReminderActive = true, nextReminderTime = updated.reminderTime)
            } else {
                updated
            }
            repository.updateTask(updatedWithReset)
            
            // Cancel alarm if marked completed
            if (updatedWithReset.isCompleted) {
                cancelReminder(updatedWithReset)
            } else if (updatedWithReset.reminderTime != null && updatedWithReset.reminderTime > System.currentTimeMillis()) {
                scheduleExactReminder(updatedWithReset)
            }
            updateWidget()
        }
    }

    /**
     * Update task details (title, description, priority, date, alarm/reminder, subtasks, category).
     */
    fun updateTaskFields(
        id: Int,
        title: String,
        description: String,
        priority: Int,
        targetDate: Calendar,
        reminderTimeMillis: Long? = null,
        repeatCount: Int = 1,
        subTasks: List<SubTask> = emptyList(),
        category: String? = null,
        reminderType: String = "notification"
    ) {
        viewModelScope.launch {
            val original = repository.getTaskById(id) ?: return@launch
            
            // Cancel old reminder if there was one
            cancelReminder(original)

            val dateStr = DateTimeUtils.formatDbDate(targetDate)
            
            // If all subtasks are complete, automatically mark the main task as complete
            val allCompleted = subTasks.isNotEmpty() && subTasks.all { it.isCompleted }
            val isMainCompleted = if (allCompleted) true else (if (original.isCompleted && !allCompleted) false else original.isCompleted)

            val updated = original.copy(
                title = title,
                description = description,
                priority = priority,
                dateAdded = dateStr,
                reminderTime = reminderTimeMillis,
                repeatCount = repeatCount,
                repeatedTimes = 0,
                isReminderActive = true,
                nextReminderTime = reminderTimeMillis,
                subTasks = subTasks,
                category = category,
                isCompleted = isMainCompleted,
                reminderType = reminderType,
                snoozedUntil = null
            )
            
            repository.updateTask(updated)
            
            // Schedule new reminder if it's active and not completed
            if (!updated.isCompleted && reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
                scheduleExactReminder(updated)
            }
            updateWidget()
        }
    }

    /**
     * Toggle a specific subtask's completion status.
     */
    fun toggleSubTaskCompleted(task: Task, subTaskIndex: Int) {
        viewModelScope.launch {
            val updatedSubTasks = task.subTasks.mapIndexed { index, sub ->
                if (index == subTaskIndex) sub.copy(isCompleted = !sub.isCompleted) else sub
            }
            val allCompleted = updatedSubTasks.isNotEmpty() && updatedSubTasks.all { it.isCompleted }
            val isMainCompleted = if (allCompleted) true else (if (task.isCompleted && !allCompleted) false else task.isCompleted)

            val updatedTask = task.copy(
                subTasks = updatedSubTasks,
                isCompleted = isMainCompleted,
                snoozedUntil = if (isMainCompleted) null else task.snoozedUntil
            )
            repository.updateTask(updatedTask)
            
            // Handle reminders sync if status changed
            if (updatedTask.isCompleted) {
                cancelReminder(updatedTask)
            } else if (updatedTask.reminderTime != null && updatedTask.reminderTime > System.currentTimeMillis()) {
                scheduleExactReminder(updatedTask)
            }

            updateWidget()
        }
    }

    /**
     * Delete task completely from the logs.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            cancelReminder(task)
            updateWidget()
        }
    }

    /**
     * Alarm Notification Scheduler.
     */
    private fun scheduleExactReminder(task: Task) {
        NotificationReceiver.scheduleExactReminder(getApplication(), task)
    }

    private fun cancelReminder(task: Task) {
        NotificationReceiver.cancelReminder(getApplication(), task)
    }

    private fun updateWidget() {
        val context = getApplication<Application>()
        val intent = Intent("com.gratus.mytodo.action.WIDGET_UPDATE").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Settings configurations.
     */
    fun setTheme(theme: String) {
        _settingsTheme.value = theme
        sharedPrefs.edit().putString("theme", theme).apply()
    }

    fun setColorScheme(scheme: String) {
        _settingsColorScheme.value = scheme
        sharedPrefs.edit().putString("color_scheme", scheme).apply()
    }

    /**
     * History Screen configuration controls.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun zoomHistory(direction: Int) {
        val target = _historyZoomLevel.value + direction
        _historyZoomLevel.value = target.coerceIn(0, 3)
    }
    
    fun setHistoryZoom(level: Int) {
        _historyZoomLevel.value = level.coerceIn(0, 3)
    }

    fun setHistoryFilter(filter: FilterOption) {
        _historyFilter.value = filter
    }

    /**
     * Real-time completion statistics.
     */
    val statsFlow: Flow<StatsData> = repository.getAllTasks()
        .map { allTasks ->
            val todayStr = DateTimeUtils.formatDbDate(System.currentTimeMillis())
            val tasksUntilToday = allTasks.filter { it.dateAdded <= todayStr }
            val total = tasksUntilToday.size
            val completed = tasksUntilToday.count { it.isCompleted }
            val completionRate = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0

            // Consistency calculation (Consecutive days with at least one completed task)
            val tasksGroupedByDate = allTasks.groupBy { it.dateAdded }
            val completedDates = tasksGroupedByDate.filter { (_, tasks) ->
                tasks.any { it.isCompleted }
            }.keys.sortedDescending()

            var streak = 0
            if (completedDates.isNotEmpty()) {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                var checkCal = Calendar.getInstance().apply { time = today.time }
                var indexDate = 0
                
                // If they completed yesterday or today, trace streak back
                val todayStr = DateTimeUtils.formatDbDate(today)
                val yesterdayCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                val yesterdayStr = DateTimeUtils.formatDbDate(yesterdayCal)

                if (completedDates.contains(todayStr) || completedDates.contains(yesterdayStr)) {
                    // Set checkCal starting date to either today (if they completed today) or yesterday (if yesterday)
                    if (!completedDates.contains(todayStr)) {
                        checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    }

                    while (true) {
                        val currentCheckStr = DateTimeUtils.formatDbDate(checkCal)
                        if (completedDates.contains(currentCheckStr)) {
                            streak++
                            checkCal.add(Calendar.DAY_OF_YEAR, -1)
                        } else {
                            break
                        }
                    }
                }
            }

            // Task completion dataset over last 7 days for graphing
            val last7DaysData = mutableListOf<DailyStats>()

            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val dateStr = DateTimeUtils.formatDbDate(cal)
                val label = DateTimeUtils.formatStatsLabel(cal)
                val dayTasks = tasksGroupedByDate[dateStr] ?: emptyList()
                val dayTotal = dayTasks.size
                val dayCompleted = dayTasks.count { it.isCompleted }
                
                last7DaysData.add(DailyStats(label, dayCompleted, dayTotal))
            }

            StatsData(total, completed, completionRate, streak, last7DaysData)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsData(0, 0, 0, 0, emptyList()))

    /**
     * Backups JSON exporting.
     */
    fun exportBackup(): String {
        return try {
            var json = ""
            runBlocking(Dispatchers.IO) {
                val allTasks = repository.getAllTasksDirect()
                json = BackupHelper.exportTasksToJson(allTasks)
            }
            json
        } catch (e: Exception) {
            Log.e("MainViewModel", "Export failed: ${e.message}")
            ""
        }
    }

    /**
     * Imports backup from a given Uri. Automatically detects format (SQLite vs JSON),
     * manages threading on Dispatchers.IO, handles database closing/replacing,
     * reschedules alarms accordingly, and invokes callbacks on the Main thread.
     */
    fun importBackupUri(uri: Uri, onComplete: (Boolean, isDb: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileBytes = inputStream.readBytes()
                    val magicString = if (fileBytes.size >= 15) {
                        String(fileBytes, 0, 15, Charsets.US_ASCII)
                    } else {
                        ""
                    }

                    if (magicString == "SQLite format 3") {
                        TaskDatabase.closeDatabase()
                        val dbFile = context.getDatabasePath("task_database")
                        val dbWalFile = context.getDatabasePath("task_database-wal")
                        val dbShmFile = context.getDatabasePath("task_database-shm")

                        dbFile.outputStream().use { output ->
                            output.write(fileBytes)
                        }

                        if (dbWalFile.exists()) dbWalFile.delete()
                        if (dbShmFile.exists()) dbShmFile.delete()

                        withContext(Dispatchers.Main) {
                            onComplete(true, true)
                        }
                    } else {
                        val jsonStr = String(fileBytes, Charsets.UTF_8)
                        val tasks = BackupHelper.importTasksFromJson(jsonStr)
                        if (tasks.isNotEmpty()) {
                            // Cancel existing alarms before replacing/merging
                            val currentTasks = repository.getAllTasksDirect()
                            for (task in currentTasks) {
                                NotificationReceiver.cancelReminder(context, task)
                            }

                            repository.insertTasks(tasks)
                            updateWidget()

                            // Reschedule all active alarms from updated DB state
                            NotificationReceiver.rescheduleAllAlarms(context)

                            withContext(Dispatchers.Main) {
                                onComplete(true, false)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onComplete(false, false)
                            }
                        }
                    }
                } ?: run {
                    withContext(Dispatchers.Main) {
                        onComplete(false, false)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Backup import from URI failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, false)
                }
            }
        }
    }

    /**
     * Force flushes Room/SQLite WAL pages to the primary .db file.
     */
    fun checkpointDatabase() {
        try {
            val db = TaskDatabase.getDatabase(getApplication())
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Checkpoint failed: ${e.message}")
        }
    }
}

/**
 * Data structures for Stats.
 */
data class StatsData(
    val totalTasks: Int,
    val completedTasks: Int,
    val completionRate: Int,
    val currentStreak: Int,
    val weeklyHistory: List<DailyStats>
)

data class DailyStats(
    val dateLabel: String,
    val completed: Int,
    val total: Int
)
