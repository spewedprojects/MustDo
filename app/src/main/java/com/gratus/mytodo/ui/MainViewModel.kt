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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import androidx.core.content.edit

/**
 * Navigation screen selection.
 */
enum class Screen {
    HOME,
    HISTORY,
    STATS,
    SETTINGS
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

    private val _colorfulHueShift = MutableStateFlow(sharedPrefs.getFloat("colorful_hue_shift", 0f))
    val colorfulHueShift: StateFlow<Float> = _colorfulHueShift.asStateFlow()

    private val _colorfulSatScale = MutableStateFlow(sharedPrefs.getFloat("colorful_sat_scale", 1.0f))
    val colorfulSatScale: StateFlow<Float> = _colorfulSatScale.asStateFlow()

    private val _lastUsedPriority = MutableStateFlow(sharedPrefs.getInt("last_priority", 1))
    val lastUsedPriority: StateFlow<Int> = _lastUsedPriority.asStateFlow()

    private val _settingsReminderInterval = MutableStateFlow(sharedPrefs.getInt("reminder_repeat_interval", 10))
    val settingsReminderInterval: StateFlow<Int> = _settingsReminderInterval.asStateFlow()

    fun setReminderInterval(minutes: Int) {
        _settingsReminderInterval.value = minutes
        sharedPrefs.edit { putInt("reminder_repeat_interval", minutes) }
    }

    private val _settingsAlarmRingtone = MutableStateFlow<String?>(sharedPrefs.getString("alarm_ringtone_uri", null))
    val settingsAlarmRingtone: StateFlow<String?> = _settingsAlarmRingtone.asStateFlow()

    fun setAlarmRingtone(uriString: String?) {
        _settingsAlarmRingtone.value = uriString
        if (uriString != null) {
            sharedPrefs.edit { putString("alarm_ringtone_uri", uriString) }
        } else {
            sharedPrefs.edit { remove("alarm_ringtone_uri") }
        }
    }

    private val _isStickyEnabled = MutableStateFlow(sharedPrefs.getBoolean("enable_sticky_tasks", true))
    val isStickyEnabled: StateFlow<Boolean> = _isStickyEnabled.asStateFlow()

    fun setStickyEnabled(enabled: Boolean) {
        _isStickyEnabled.value = enabled
        sharedPrefs.edit {putBoolean("enable_sticky_tasks", enabled) }
        NotificationReceiver.rescheduleAllAlarms(getApplication())
        updateWidget()
    }

    // Category / Tag Management
    private val defaultCategories = listOf("Sticky", "Personal", "Work", "Errands", "Health", "Learning")
    private val _customCategories = MutableStateFlow<List<String>>(
        sharedPrefs.getStringSet("custom_categories", emptySet())?.toList()?.sorted() ?: emptyList()
    )
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    val categories: StateFlow<List<String>> = combine(_customCategories, _isStickyEnabled) { custom, stickyEnabled ->
        val base = if (stickyEnabled) defaultCategories else defaultCategories.filter { it != "Sticky" }
        (base + custom).distinct()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultCategories)

    fun addCustomCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return
        val current = _customCategories.value.toMutableList()
        if (!current.contains(trimmed) && !defaultCategories.contains(trimmed)) {
            current.add(trimmed)
            current.sort()
            _customCategories.value = current
            sharedPrefs.edit { putStringSet("custom_categories", current.toSet()) }
        }
    }

    fun deleteCustomCategory(category: String) {
        val current = _customCategories.value.toMutableList()
        if (current.remove(category)) {
            _customCategories.value = current
            sharedPrefs.edit { putStringSet("custom_categories", current.toSet()) }
            
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

    private val _isFullScreenPermissionGranted = MutableStateFlow(true)
    val isFullScreenPermissionGranted: StateFlow<Boolean> = _isFullScreenPermissionGranted.asStateFlow()

    fun checkPermissions(context: Context) {
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val areEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            hasPermission && areEnabled
        } else {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        _isNotificationPermissionGranted.value = notificationGranted

        val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            try {
                val granted = alarmManager.canScheduleExactAlarms()
                Log.d("MainViewModel", "canScheduleExactAlarms returned: $granted")
                granted
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error checking canScheduleExactAlarms: ${e.message}", e)
                false
            }
        } else {
            true
        }
        _isAlarmPermissionGranted.value = alarmGranted

        val fullScreenGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            if (notifManager != null) {
                try {
                    val method = notifManager.javaClass.getMethod("canUseFullScreenIntent")
                    val granted = method.invoke(notifManager) as? Boolean ?: true
                    Log.d("MainViewModel", "canUseFullScreenIntent returned: $granted")
                    granted
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Could not query canUseFullScreenIntent: ${e.message}")
                    true
                }
            } else {
                true
            }
        } else {
            true
        }
        _isFullScreenPermissionGranted.value = fullScreenGranted
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

    private val _highlightedTaskId = MutableStateFlow<Int?>(null)
    val highlightedTaskId: StateFlow<Int?> = _highlightedTaskId.asStateFlow()

    private var highlightJob: kotlinx.coroutines.Job? = null

    fun setHighlightedTaskId(id: Int?) {
        highlightJob?.cancel()
        _highlightedTaskId.value = id
        if (id != null) {
            highlightJob = viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                if (_highlightedTaskId.value == id) {
                    _highlightedTaskId.value = null
                }
            }
        }
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
    @OptIn(ExperimentalCoroutinesApi::class)
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
     * Set of date strings (yyyy-MM-dd) that contain active scheduled tasks.
     */
    val taskDates: StateFlow<Set<String>> = repository.getAllTasks()
        .map { tasks -> tasks.filter { !it.isCompleted }.map { it.dateAdded }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /**
     * Get tasks list flow for a specific date (used by the smooth sliding HorizontalPager).
     */
    fun getTasksForDateFlow(dateStr: String): Flow<List<Task>> {
        return repository.getAllTasks()
            .map { allTasks ->
                if (!_isStickyEnabled.value) {
                    return@map allTasks.filter { it.dateAdded == dateStr && !it.category.equals("Sticky", ignoreCase = true) }
                }

                val directTasksForDate = allTasks.filter { it.dateAdded == dateStr }
                val stickyGroups = allTasks
                    .filter { it.category?.equals("Sticky", ignoreCase = true) == true }
                    .groupBy { it.title.trim().lowercase(java.util.Locale.ROOT) }

                val combined = directTasksForDate.toMutableList()

                stickyGroups.forEach { (_, instances) ->
                    // Find all instances that are part of an active series covering dateStr
                    val activeCandidates = instances.filter {
                        it.dateAdded <= dateStr &&
                        (it.terminatedDate.isNullOrBlank() || dateStr <= it.terminatedDate)
                    }
                    if (activeCandidates.isEmpty()) return@forEach

                    // The master for this active series is the earliest instance in the active series
                    val master = activeCandidates.minByOrNull { it.dateAdded } ?: return@forEach
                    val isEveryday = master.deadlineDate.isNullOrBlank()
                    val deadlineDate = master.deadlineDate

                    // If deadline task, cannot appear after deadlineDate
                    if (!isEveryday && deadlineDate != null && dateStr > deadlineDate) return@forEach

                    // Check if already present in direct tasks for this date (by title regardless of category)
                    val alreadyHasDirect = directTasksForDate.any {
                        it.title.trim().equals(master.title.trim(), ignoreCase = true)
                    }

                    if (!alreadyHasDirect) {
                        if (isEveryday) {
                            // Everyday task: Always generates a fresh, uncompleted virtual task for dateStr
                            combined.add(
                                master.copy(
                                    id = 0,
                                    dateAdded = dateStr,
                                    isCompleted = false,
                                    repeatedTimes = 0,
                                    subTasks = master.subTasks.map { it.copy(isCompleted = false) },
                                    snoozedUntil = null,
                                    nextReminderTime = master.reminderTime
                                )
                            )
                        } else {
                            // Deadline task: If completed on any prior day (< dateStr), stops repeating on future days
                            val completedPrior = activeCandidates.any { it.dateAdded < dateStr && it.isCompleted }
                            if (!completedPrior) {
                                combined.add(
                                    master.copy(
                                        id = 0,
                                        dateAdded = dateStr,
                                        isCompleted = false,
                                        repeatedTimes = 0,
                                        subTasks = master.subTasks.map { it.copy(isCompleted = false) },
                                        snoozedUntil = null,
                                        nextReminderTime = master.reminderTime
                                    )
                                )
                            }
                        }
                    }
                }
                combined
            }
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
    @OptIn(ExperimentalCoroutinesApi::class)
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
        sharedPrefs.edit { putString("sort_option", next.name) }
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
            val computedDeadline: String? = if (category?.equals("Sticky", ignoreCase = true) == true) {
                if (replicateDates.isNotEmpty()) {
                    replicateDates.maxOrNull()
                } else if (everydayDaysCount > 0) {
                    val cal = Calendar.getInstance().apply {
                        time = targetDate.time
                        add(Calendar.DAY_OF_YEAR, everydayDaysCount)
                    }
                    DateTimeUtils.formatDbDate(cal)
                } else {
                    null
                }
            } else null

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
                snoozedUntil = null,
                deadlineDate = computedDeadline
            )

            // Save last used priority
            _lastUsedPriority.value = priority
            sharedPrefs.edit { putInt("last_priority", priority) }

            val baseId = repository.insertTask(baseTask).toInt()
            
            // Set alarm if custom reminder was scheduled
            if (reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
                val scheduledTask = baseTask.copy(id = baseId)
                scheduleExactReminder(scheduledTask)
            }

            // Replicate to custom selected future dates (bypassed for Sticky category)
            if (!category.equals("Sticky", ignoreCase = true)) {
                replicateDates.forEach { futureDate ->
                    if (futureDate != dateStr) {
                        val futureReminderTime: Long? = if (reminderTimeMillis != null) {
                            val futureCal = Calendar.getInstance().apply {
                                time = DateTimeUtils.parseDbDate(futureDate) ?: Date()
                                val origCal = Calendar.getInstance().apply { timeInMillis = reminderTimeMillis }
                                set(Calendar.HOUR_OF_DAY, origCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, origCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            futureCal.timeInMillis
                        } else null

                        val futureTask = baseTask.copy(
                            dateAdded = futureDate,
                            isRecurring = false,
                            reminderTime = futureReminderTime,
                            nextReminderTime = futureReminderTime,
                            reminderType = reminderType,
                            snoozedUntil = null
                        )
                        val newId = repository.insertTask(futureTask).toInt()
                        if (futureReminderTime != null && futureReminderTime > System.currentTimeMillis()) {
                            scheduleExactReminder(futureTask.copy(id = newId))
                        }
                    }
                }
            }

            // Replicate automatically to "everyday" range (bypassed for Sticky category)
            if (everydayDaysCount > 0 && !category.equals("Sticky", ignoreCase = true)) {
                for (i in 1..everydayDaysCount) {
                    val runCal = Calendar.getInstance().apply {
                        time = targetDate.time
                        add(Calendar.DAY_OF_YEAR, i)
                    }
                    val dailyStr = DateTimeUtils.formatDbDate(runCal)
                    val futureReminderTime: Long? = if (reminderTimeMillis != null) {
                        val futureCal = Calendar.getInstance().apply {
                            time = runCal.time
                            val origCal = Calendar.getInstance().apply { timeInMillis = reminderTimeMillis }
                            set(Calendar.HOUR_OF_DAY, origCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, origCal.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        futureCal.timeInMillis
                    } else null

                    val everydayTask = baseTask.copy(
                        dateAdded = dailyStr,
                        isRecurring = true,
                        reminderTime = futureReminderTime,
                        nextReminderTime = futureReminderTime,
                        reminderType = reminderType,
                        snoozedUntil = null
                    )
                    val newId = repository.insertTask(everydayTask).toInt()
                    if (futureReminderTime != null && futureReminderTime > System.currentTimeMillis()) {
                        scheduleExactReminder(everydayTask.copy(id = newId))
                    }
                }
            }

            updateWidget()
        }
    }

    /**
     * Terminate a Sticky task forever from current date onward.
     */
    fun terminateStickyTaskForever(task: Task) {
        viewModelScope.launch {
            val targetTitle = task.title.trim()
            val currentDateAdded = task.dateAdded
            val allTasks = repository.getAllTasksDirect()

            val matchingSticky = allTasks.filter {
                it.category?.equals("Sticky", ignoreCase = true) == true &&
                it.title.trim().equals(targetTitle, ignoreCase = true)
            }

            // Set terminatedDate to currentDateAdded on master and historical instances, mark completed
            matchingSticky.filter { it.dateAdded <= currentDateAdded }.forEach { t ->
                val updated = t.copy(
                    isCompleted = true,
                    isReminderActive = false,
                    snoozedUntil = null,
                    terminatedDate = currentDateAdded
                )
                repository.updateTask(updated)
                NotificationReceiver.cancelReminder(getApplication(), t)
            }

            // Delete any future occurrences (> currentDateAdded)
            matchingSticky.filter { it.dateAdded > currentDateAdded }.forEach { t ->
                repository.deleteTask(t)
                NotificationReceiver.cancelReminder(getApplication(), t)
            }

            // If this was a virtual instance (id == 0), insert a completed instance for currentDateAdded if not already present
            if (task.id == 0) {
                val hasDirectForCurrentDate = matchingSticky.any { it.dateAdded == currentDateAdded }
                if (!hasDirectForCurrentDate) {
                    val terminatedInstance = task.copy(
                        isCompleted = true,
                        isReminderActive = false,
                        snoozedUntil = null,
                        terminatedDate = currentDateAdded
                    )
                    repository.insertTask(terminatedInstance)
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

            if (task.id == 0) {
                val newId = repository.insertTask(updatedWithReset).toInt()
                val insertedTask = updatedWithReset.copy(id = newId)
                if (insertedTask.isCompleted) {
                    cancelReminder(insertedTask)
                } else if (insertedTask.reminderTime != null && insertedTask.reminderTime > System.currentTimeMillis()) {
                    scheduleExactReminder(insertedTask)
                }
            } else {
                repository.updateTask(updatedWithReset)
                if (updatedWithReset.isCompleted) {
                    cancelReminder(updatedWithReset)
                } else if (updatedWithReset.reminderTime != null && updatedWithReset.reminderTime > System.currentTimeMillis()) {
                    scheduleExactReminder(updatedWithReset)
                }
            }
            updateWidget()
        }
    }

    /**
     * Update task details (title, description, priority, date, alarm/reminder, subtasks, category).
     */
    /**
     * Update task details (title, description, priority, date, alarm/reminder, subtasks, category, replication).
     * Handles transitions between Sticky and Normal categories, and virtual sticky tasks (id == 0).
     */
    fun updateTaskFields(
        taskToEdit: Task,
        title: String,
        description: String,
        priority: Int,
        targetDate: Calendar,
        reminderTimeMillis: Long? = null,
        repeatCount: Int = 1,
        subTasks: List<SubTask> = emptyList(),
        category: String? = null,
        reminderType: String = "notification",
        replicateDates: List<String> = emptyList(),
        everydayCount: Int = 0
    ) {
        viewModelScope.launch {
            val dateStr = DateTimeUtils.formatDbDate(targetDate)
            val wasSticky = taskToEdit.category?.equals("Sticky", ignoreCase = true) == true
            val isStickyNow = category?.equals("Sticky", ignoreCase = true) == true

            val allCompleted = subTasks.isNotEmpty() && subTasks.all { it.isCompleted }
            val isMainCompleted = if (allCompleted) true else (if (taskToEdit.isCompleted && !allCompleted) false else taskToEdit.isCompleted)

            if (wasSticky && !isStickyNow) {
                // CASE 1: Converting Sticky -> Non-Sticky
                val allTasks = repository.getAllTasksDirect()
                val matchingSticky = allTasks.filter {
                    it.category?.equals("Sticky", ignoreCase = true) == true &&
                    it.title.trim().equals(taskToEdit.title.trim(), ignoreCase = true)
                }

                // The sticky recurrence was active up to the day before dateStr.
                // Terminate past instances (< dateStr) with terminatedDate = previous day.
                val prevDayCal = (targetDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                val terminationDate = DateTimeUtils.formatDbDate(prevDayCal)

                matchingSticky.filter { it.dateAdded < dateStr }.forEach { t ->
                    val updated = t.copy(
                        terminatedDate = terminationDate
                    )
                    repository.updateTask(updated)
                }

                // Delete any sticky occurrences on or after dateStr (>= dateStr)
                matchingSticky.filter { it.dateAdded >= dateStr }.forEach { t ->
                    repository.deleteTask(t)
                    cancelReminder(t)
                }

                // Always insert the new standalone normal task on dateStr
                val newNormalTask = Task(
                    id = 0,
                    title = title,
                    description = description,
                    priority = priority,
                    dateAdded = dateStr,
                    reminderTime = reminderTimeMillis,
                    isRecurring = false,
                    createdSeq = System.currentTimeMillis(),
                    repeatCount = repeatCount,
                    repeatedTimes = 0,
                    isReminderActive = true,
                    nextReminderTime = reminderTimeMillis,
                    subTasks = subTasks,
                    category = category,
                    reminderType = reminderType,
                    isCompleted = isMainCompleted,
                    snoozedUntil = null,
                    deadlineDate = null,
                    terminatedDate = null
                )
                val newId = repository.insertTask(newNormalTask).toInt()
                val inserted = newNormalTask.copy(id = newId)
                if (!inserted.isCompleted && reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
                    scheduleExactReminder(inserted)
                }
            } else if (!wasSticky && isStickyNow) {
                // CASE 2: Converting Non-Sticky -> Sticky
                cancelReminder(taskToEdit)

                val computedDeadline: String? = if (replicateDates.isNotEmpty()) {
                    replicateDates.maxOrNull()
                } else if (everydayCount > 0) {
                    val cal = Calendar.getInstance().apply {
                        time = targetDate.time
                        add(Calendar.DAY_OF_YEAR, everydayCount)
                    }
                    DateTimeUtils.formatDbDate(cal)
                } else {
                    null
                }

                val updatedStickyTask = taskToEdit.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    dateAdded = dateStr,
                    reminderTime = reminderTimeMillis,
                    isRecurring = everydayCount > 0,
                    repeatCount = repeatCount,
                    repeatedTimes = 0,
                    isReminderActive = true,
                    nextReminderTime = reminderTimeMillis,
                    subTasks = subTasks,
                    category = "Sticky",
                    reminderType = reminderType,
                    isCompleted = isMainCompleted,
                    snoozedUntil = null,
                    deadlineDate = computedDeadline,
                    terminatedDate = null
                )
                repository.updateTask(updatedStickyTask)
                if (!updatedStickyTask.isCompleted && reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
                    scheduleExactReminder(updatedStickyTask)
                }
            } else if (wasSticky && isStickyNow) {
                // CASE 3: Editing existing Sticky Task
                val allTasks = repository.getAllTasksDirect()
                val matchingSticky = allTasks.filter {
                    it.category?.equals("Sticky", ignoreCase = true) == true &&
                    it.title.trim().equals(taskToEdit.title.trim(), ignoreCase = true)
                }

                val computedDeadline: String? = if (replicateDates.isNotEmpty()) {
                    replicateDates.maxOrNull()
                } else if (everydayCount > 0) {
                    val cal = Calendar.getInstance().apply {
                        time = targetDate.time
                        add(Calendar.DAY_OF_YEAR, everydayCount)
                    }
                    DateTimeUtils.formatDbDate(cal)
                } else {
                    taskToEdit.deadlineDate
                }

                if (taskToEdit.id == 0) {
                    // Virtual task instance: update master and all matching instances with new details
                    matchingSticky.forEach { t ->
                        val updated = t.copy(
                            title = title,
                            description = description,
                            priority = priority,
                            reminderTime = reminderTimeMillis,
                            repeatCount = repeatCount,
                            reminderType = reminderType,
                            subTasks = subTasks,
                            deadlineDate = computedDeadline
                        )
                        repository.updateTask(updated)
                    }
                } else {
                    // Direct instance: update this row and sync master template properties if needed
                    val updated = taskToEdit.copy(
                        title = title,
                        description = description,
                        priority = priority,
                        dateAdded = dateStr,
                        reminderTime = reminderTimeMillis,
                        repeatCount = repeatCount,
                        subTasks = subTasks,
                        category = "Sticky",
                        isCompleted = isMainCompleted,
                        reminderType = reminderType,
                        snoozedUntil = null,
                        deadlineDate = computedDeadline
                    )
                    cancelReminder(taskToEdit)
                    repository.updateTask(updated)
                    if (!updated.isCompleted && reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
                        scheduleExactReminder(updated)
                    }
                }
            } else {
                // CASE 4: Regular Task Edit (Non-Sticky -> Non-Sticky)
                val original = repository.getTaskById(taskToEdit.id)
                if (original != null) {
                    cancelReminder(original)
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
                    if (!updated.isCompleted && reminderTimeMillis != null && reminderTimeMillis > System.currentTimeMillis()) {
                        scheduleExactReminder(updated)
                    }
                }
            }

            NotificationReceiver.rescheduleAllAlarms(getApplication())
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
            if (task.id == 0) {
                val newId = repository.insertTask(updatedTask).toInt()
                val insertedTask = updatedTask.copy(id = newId)
                if (insertedTask.isCompleted) {
                    cancelReminder(insertedTask)
                } else if (insertedTask.reminderTime != null && insertedTask.reminderTime > System.currentTimeMillis()) {
                    scheduleExactReminder(insertedTask)
                }
            } else {
                repository.updateTask(updatedTask)
                if (updatedTask.isCompleted) {
                    cancelReminder(updatedTask)
                } else if (updatedTask.reminderTime != null && updatedTask.reminderTime > System.currentTimeMillis()) {
                    scheduleExactReminder(updatedTask)
                }
            }

            updateWidget()
        }
    }

    /**
     * Delete task completely from the logs.
     * If task is a Sticky task, wipes master and all occurrences completely from the DB.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (task.category?.equals("Sticky", ignoreCase = true) == true) {
                val targetTitle = task.title.trim()
                val allTasks = repository.getAllTasksDirect()
                val matchingSticky = allTasks.filter {
                    it.category?.equals("Sticky", ignoreCase = true) == true &&
                    it.title.trim().equals(targetTitle, ignoreCase = true)
                }
                if (matchingSticky.isNotEmpty()) {
                    matchingSticky.forEach { t ->
                        repository.deleteTask(t)
                        cancelReminder(t)
                    }
                } else if (task.id != 0) {
                    repository.deleteTask(task)
                    cancelReminder(task)
                }
            } else {
                repository.deleteTask(task)
                cancelReminder(task)
            }
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
        sharedPrefs.edit { putString("theme", theme) }
    }

    fun setColorScheme(scheme: String) {
        _settingsColorScheme.value = scheme
        sharedPrefs.edit { putString("color_scheme", scheme) }
    }

    fun setColorfulHueShift(shift: Float) {
        _colorfulHueShift.value = shift
        sharedPrefs.edit { putFloat("colorful_hue_shift", shift) }
    }

    fun setColorfulSatScale(scale: Float) {
        _colorfulSatScale.value = scale
        sharedPrefs.edit { putFloat("colorful_sat_scale", scale) }
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
