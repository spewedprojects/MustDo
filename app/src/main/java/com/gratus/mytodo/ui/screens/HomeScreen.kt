@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.gratus.mytodo.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.components.TaskAddDialog
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.*
import com.gratus.mytodo.ui.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * HomeScreen displays the current date's tasks, supporting the date swipe gesture.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    colorSchemeType: String
) {
    val currentDate by viewModel.currentDate.collectAsState()
    val lastUsedPriority by viewModel.lastUsedPriority.collectAsState()

    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val taskToEdit by viewModel.taskToEdit.collectAsState()
    val taskToDelete by viewModel.taskToDelete.collectAsState()

    HomeScreenContent(
        currentDate = currentDate,
        lastUsedPriority = lastUsedPriority,
        colorSchemeType = colorSchemeType,
        showAddDialog = showAddDialog,
        taskToEdit = taskToEdit,
        taskToDelete = taskToDelete,
        onShowAddDialogChange = { viewModel.setShowAddDialog(it) },
        onTaskToEditChange = { viewModel.setTaskToEdit(it) },
        onTaskToDeleteChange = { viewModel.setTaskToDelete(it) },
        onNavigateDate = { viewModel.navigateDate(it) },
        onSetDate = { viewModel.setDate(it) },
        onToggleComplete = { viewModel.toggleCompleted(it) },
        onDeleteTask = { viewModel.deleteTask(it) },
        onAddTask = { t, d, p, targetDate, replicateDates, everydayCount, reminderTimeMillis ->
            viewModel.addTask(t, d, p, targetDate, replicateDates, everydayCount, reminderTimeMillis)
        },
        onEditTask = { task, t, d, p, targetDate, reminderTimeMillis ->
            viewModel.updateTaskFields(task.id, t, d, p, targetDate, reminderTimeMillis)
        },
        getTasksForDate = { dateStr -> viewModel.getTasksForDateFlow(dateStr) }
    )
}

/**
 * Stateless version of HomeScreen for preview and testing.
 */
@Composable
fun HomeScreenContent(
    currentDate: Calendar,
    lastUsedPriority: Int,
    colorSchemeType: String,
    showAddDialog: Boolean,
    taskToEdit: Task?,
    taskToDelete: Task?,
    onShowAddDialogChange: (Boolean) -> Unit,
    onTaskToEditChange: (Task?) -> Unit,
    onTaskToDeleteChange: (Task?) -> Unit,
    onNavigateDate: (Int) -> Unit,
    onSetDate: (Calendar) -> Unit,
    onToggleComplete: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTask: (String, String, Int, Calendar, List<String>, Int, Long?) -> Unit,
    onEditTask: (Task, String, String, Int, Calendar, Long?) -> Unit,
    getTasksForDate: (String) -> Flow<List<Task>>
) {
    val context = LocalContext.current

    // Smooth Sliding Pager setup
    val baseDate = remember { Calendar.getInstance() }
    val initialPage = 10000
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 20000 })

    val daysDiff = remember(currentDate) {
        DateTimeUtils.daysBetween(baseDate, currentDate)
    }
    val targetPage = initialPage + daysDiff

    // Scroll to page when currentDate changes externally (arrows, picker)
    LaunchedEffect(targetPage) {
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync focus date when page changes via swipe gesture
    LaunchedEffect(pagerState.currentPage) {
        val diff = pagerState.currentPage - initialPage
        val targetCal = (baseDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, diff)
        }
        if (!DateTimeUtils.isSameDay(targetCal, currentDate)) {
            onSetDate(targetCal)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Screen Header Label Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (colorSchemeType == "simple") MaterialTheme.colorScheme.surface 
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                    .clickable {
                        // Launch full native datepicker when tapping focused date card
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                onSetDate(cal)
                            },
                            currentDate.get(Calendar.YEAR),
                            currentDate.get(Calendar.MONTH),
                            currentDate.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Pick a Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = DateTimeUtils.formatHomeDateLabel(currentDate),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " (Tap to Pick)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Smooth sliding horizontal pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageDiff = page - initialPage
                val pageDate = remember(page) {
                    (baseDate.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, pageDiff)
                    }
                }
                val dateStr = remember(pageDate) { DateTimeUtils.formatDbDate(pageDate) }
                val pageTasks by remember(dateStr) { getTasksForDate(dateStr) }
                    .collectAsState(initial = emptyList())

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    if (pageTasks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "No tasks",
                                    modifier = Modifier
                                        .size(82.dp)
                                        .alpha(0.3f),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "No tasks recorded for today",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Swipe horizontally or tap Quick-Add to start!",
                                    fontSize = AppFontSizes.small,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("home_tasks_list"),
                            contentPadding = PaddingValues(bottom = 80.dp), // Clear bottom FAB space
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pageTasks, key = { it.id }) { task ->
                                 TaskItemCard(
                                     task = task,
                                     colorSchemeType = colorSchemeType,
                                     onToggleComplete = { onToggleComplete(task) },
                                     onDelete = { onTaskToDeleteChange(task) },
                                     onLongClick = { onTaskToEditChange(task) }
                                 )
                            }
                        }
                    }
                }
            }
        }

        // Quick Add Floating Button
        FloatingActionButton(
            onClick = { onShowAddDialogChange(true) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("quick_add_fab"),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    // Task Adding Overlay Dialog box configuration
    if (showAddDialog) {
        TaskAddDialog(
            initialDate = currentDate,
            lastUsedPriority = lastUsedPriority,
            onDismiss = { onShowAddDialogChange(false) },
            onConfirm = { t, d, p, targetDate, replicateDates, everydayCount, reminderTimeMillis ->
                onAddTask(t, d, p, targetDate, replicateDates, everydayCount, reminderTimeMillis)
                onShowAddDialogChange(false)
                Toast.makeText(context, "Task created!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Task Editing Dialog Box
    if (taskToEdit != null) {
        TaskAddDialog(
            initialDate = Calendar.getInstance().apply {
                time = DateTimeUtils.parseDbDate(taskToEdit.dateAdded) ?: Date()
            },
            lastUsedPriority = lastUsedPriority,
            taskToEdit = taskToEdit,
            onDismiss = { onTaskToEditChange(null) },
            onConfirm = { t, d, p, targetDate, _, _, reminderTimeMillis ->
                onEditTask(taskToEdit, t, d, p, targetDate, reminderTimeMillis)
                onTaskToEditChange(null)
                Toast.makeText(context, "Task updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Task Confirmation Dialog
    if (taskToDelete != null) {
         AlertDialog(
            onDismissRequest = { onTaskToDeleteChange(null) },
            modifier = Modifier.border(
                width = 1.dp,
                color = if (colorSchemeType == "simple") {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                } else if (colorSchemeType == "minimal") {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(28.dp)
            ),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete Task", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete this task?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskToDelete?.let { onDeleteTask(it) }
                        onTaskToDeleteChange(null)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onTaskToDeleteChange(null) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

/**
 * Task item card component.
 */
@Composable
fun TaskItemCard(
    task: Task,
    colorSchemeType: String,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit
) {
    val isCompleted = task.isCompleted
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCompleted) 0.8f else 1.0f) // Greys out completely when marked completed
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {}, // No action on single card tap to avoid checkmark interference
                onLongClick = onLongClick
            )
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                if (colorSchemeType == "minimal") {
                    if (isDark) Color(0x15FFFFFF) else Color(0x33B0AAB9)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                }
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = when (colorSchemeType) {
            "simple" -> borderStrokeSimple(isCompleted)
            "minimal" -> {
                if (isCompleted) {
                    androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder)
                }
            }
            else -> null
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mark Completed icon button
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark done status",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Mark done status",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title & Description (Markdown parsed dynamically)
            Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parseStyledDescription(task.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCompleted) 0.5f else 0.8f)
                    )
                }

                // Show Scheduled Alarm timestamp indicator
                if (task.reminderTime != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Active reminder",
                            tint = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                         Text(
                            text = "Alert scheduled: " + DateTimeUtils.formatAlarmTime(context, task.reminderTime),
                            fontSize = AppFontSizes.micro,
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Priority level displayed inside a boxed container colored based on active colorSchemeType
            val badgeStyle = if (colorSchemeType == "minimal") {
                getMinimalPriorityColors(task.priority, isCompleted, isDark)
            } else {
                val containerCol = getPriorityBoxColor(task.priority, isCompleted)
                val textCol = if (task.priority == 4 || isCompleted) Color.DarkGray else Color.White
                val borderCol = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                PriorityThemeBadgeColors(containerCol, textCol, borderCol)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeStyle.containerColor)
                    .border(
                        1.dp,
                        badgeStyle.borderColor,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                 Text(
                     text = task.priority.toString(),
                     fontWeight = FontWeight.Bold,
                     fontSize = AppFontSizes.large,
                     color = badgeStyle.contentColor
                 )
            }

            // Delete item button inside row
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Custom Simple B&W border calculation.
 */
@Composable
fun borderStrokeSimple(isCompleted: Boolean): androidx.compose.foundation.BorderStroke {
    val color = if (isCompleted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }
    return androidx.compose.foundation.BorderStroke(1.dp, color)
}

/**
 * Returns prioritized color code from Urgent level 1 down to soft Low level 4.
 */
fun getPriorityBoxColor(priority: Int, isCompleted: Boolean): Color {
    if (isCompleted) return Color.LightGray.copy(alpha = 0.5f)
    return when (priority) {
        1 -> PriorityRed
        2 -> PriorityOrange
        3 -> PriorityAmber
        4 -> PriorityYellow
        else -> Color.Gray
    }
}

@Preview(showBackground = true, name = "HomeScreen - Minimal Theme", showSystemUi = true)
@Composable
fun HomeScreenMinimalPreview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
        HomeScreenContent(
            currentDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            colorSchemeType = "minimal",
            showAddDialog = false,
            taskToEdit = null,
            taskToDelete = null,
            onShowAddDialogChange = {},
            onTaskToEditChange = {},
            onTaskToDeleteChange = {},
            onNavigateDate = {},
            onSetDate = {},
            onToggleComplete = {},
            onDeleteTask = {},
            onAddTask = { _, _, _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(sampleTasks) }
        )
    }
}

@Preview(showBackground = true, name = "HomeScreen - Simple Theme")
@Composable
fun HomeScreenSimplePreview() {
    SoftTodoTheme(colorSchemeType = "simple") {
        HomeScreenContent(
            currentDate = Calendar.getInstance(),
            lastUsedPriority = 1,
            colorSchemeType = "simple",
            showAddDialog = false,
            taskToEdit = null,
            taskToDelete = null,
            onShowAddDialogChange = {},
            onTaskToEditChange = {},
            onTaskToDeleteChange = {},
            onNavigateDate = {},
            onSetDate = {},
            onToggleComplete = {},
            onDeleteTask = {},
            onAddTask = { _, _, _, _, _, _, _ -> },
            onEditTask = { _, _, _, _, _, _ -> },
            getTasksForDate = { _ -> kotlinx.coroutines.flow.flowOf(sampleTasks) }
        )
    }
}

@Preview(showBackground = true, name = "Task Item Card")
@Composable
fun TaskItemCardPreview() {
    SoftTodoTheme(colorSchemeType = "colorful") {
        TaskItemCard(
            task = sampleTasks[0],
            colorSchemeType = "colorful",
            onToggleComplete = {},
            onDelete = {},
            onLongClick = {}
        )
    }
}

private val sampleTasks = listOf(
    Task(id = 1, title = "Finish Project Proposal", description = "Finalize the budget and timeline", priority = 1, dateAdded = "2023-10-27", isCompleted = false),
    Task(id = 2, title = "Grocery Shopping", description = "Milk, Eggs, Bread, Fruits", priority = 2, dateAdded = "2023-10-27", isCompleted = true),
    Task(id = 3, title = "Gym Workout", description = "Leg day", priority = 3, dateAdded = "2023-10-27", isCompleted = false)
)
