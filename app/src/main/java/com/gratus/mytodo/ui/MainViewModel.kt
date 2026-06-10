package com.gratus.mytodo.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gratus.mytodo.components.NotificationReceiver
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.TaskDatabase
import com.gratus.mytodo.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
 * Historical screen display modes.
 */
enum class DisplayType {
    LIST,
    GROUPED
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

    // Dialog / Edit Screen States (preserved across screen rotations)
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _taskToEdit = MutableStateFlow<Task?>(null)
    val taskToEdit: StateFlow<Task?> = _taskToEdit.asStateFlow()

    private val _taskToDelete = MutableStateFlow<Task?>(null)
    val taskToDelete: StateFlow<Task?> = _taskToDelete.asStateFlow()

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

    private val _historyZoomLevel = MutableStateFlow(3) // 0 (Year), 1 (Month), 2 (Week), 3 (Regular)
    val historyZoomLevel: StateFlow<Int> = _historyZoomLevel.asStateFlow()

    private val _historyDisplayType = MutableStateFlow(DisplayType.LIST)
    val historyDisplayType: StateFlow<DisplayType> = _historyDisplayType.asStateFlow()

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
        repeatCount: Int = 1
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
                repeatedTimes = 0
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
                    val futureTask = baseTask.copy(dateAdded = futureDate, isRecurring = false, reminderTime = null)
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
                    val everydayTask = baseTask.copy(dateAdded = dailyStr, isRecurring = true, reminderTime = null)
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
            val updated = task.copy(isCompleted = !task.isCompleted)
            val updatedWithReset = if (!updated.isCompleted) updated.copy(repeatedTimes = 0) else updated
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
     * Update task details (title, description, priority, date, alarm/reminder).
     */
    fun updateTaskFields(
        id: Int,
        title: String,
        description: String,
        priority: Int,
        targetDate: Calendar,
        reminderTimeMillis: Long? = null,
        repeatCount: Int = 1
    ) {
        viewModelScope.launch {
            val original = repository.getTaskById(id) ?: return@launch
            
            // Cancel old reminder if there was one
            cancelReminder(original)

            val dateStr = DateTimeUtils.formatDbDate(targetDate)
            val updated = original.copy(
                title = title,
                description = description,
                priority = priority,
                dateAdded = dateStr,
                reminderTime = reminderTimeMillis,
                repeatCount = repeatCount,
                repeatedTimes = 0
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

    fun setHistoryDisplay(type: DisplayType) {
        _historyDisplayType.value = type
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
     * Backups JSON importing.
     */
    fun importBackup(jsonStr: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val tasks = BackupHelper.importTasksFromJson(jsonStr)
                if (tasks.isNotEmpty()) {
                    repository.insertTasks(tasks)
                    updateWidget()
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Import failed: ${e.message}")
                onComplete(false)
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

    /**
     * Closes the active database, replaces task_database with the backup, and deletes WAL/SHM pages.
     */
    fun importDbBackup(inputStream: java.io.InputStream, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                TaskDatabase.closeDatabase()
                val dbFile = getApplication<Application>().getDatabasePath("task_database")
                val dbWalFile = getApplication<Application>().getDatabasePath("task_database-wal")
                val dbShmFile = getApplication<Application>().getDatabasePath("task_database-shm")
                
                dbFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                
                if (dbWalFile.exists()) dbWalFile.delete()
                if (dbShmFile.exists()) dbShmFile.delete()
                
                onComplete(true)
            } catch (e: Exception) {
                Log.e("MainViewModel", "DB Import failed: ${e.message}", e)
                onComplete(false)
            }
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
