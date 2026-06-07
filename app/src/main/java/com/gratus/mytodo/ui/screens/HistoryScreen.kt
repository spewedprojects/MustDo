package com.gratus.mytodo.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.DisplayType
import com.gratus.mytodo.ui.FilterOption
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.*
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.*

/**
 * Historical records screen with filtering, date classification, and structural pinch-to-zoom.
 */
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    colorSchemeType: String
) {
    val tasks by viewModel.historyTasks.collectAsState(initial = emptyList())
    val query by viewModel.searchQuery.collectAsState()
    val zoomLevel by viewModel.historyZoomLevel.collectAsState()
    val displayType by viewModel.historyDisplayType.collectAsState()
    val activeFilter by viewModel.historyFilter.collectAsState()

    HistoryScreenContent(
        tasks = tasks,
        query = query,
        zoomLevel = zoomLevel,
        displayType = displayType,
        activeFilter = activeFilter,
        colorSchemeType = colorSchemeType,
        onQueryChange = { viewModel.setSearchQuery(it) },
        onZoomChange = { viewModel.zoomHistory(it) },
        onDisplayTypeChange = { viewModel.setHistoryDisplay(it) },
        onFilterChange = { viewModel.setHistoryFilter(it) }
    )
}

/**
 * Stateless version of HistoryScreen for preview and testing.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(
    tasks: List<Task>,
    query: String,
    zoomLevel: Int,
    displayType: DisplayType,
    activeFilter: FilterOption,
    colorSchemeType: String,
    onQueryChange: (String) -> Unit,
    onZoomChange: (Int) -> Unit,
    onDisplayTypeChange: (DisplayType) -> Unit,
    onFilterChange: (FilterOption) -> Unit
) {
    val context = LocalContext.current

    // State to throttling scale gestures
    var lastGestureTime by remember { mutableLongStateOf(0L) }

    // Pinch to Zoom math utilizing pointers transform detector (bounds checked in ViewModel)
    val pinchZoomModifier = Modifier.pointerInput(Unit) {
        detectTransformGestures { _, _, zoom, _ ->
            val now = System.currentTimeMillis()
            if (now - lastGestureTime < 150) return@detectTransformGestures // Debounce rate
            if (zoom > 1.25f) {
                onZoomChange(1) // zoom in
                lastGestureTime = now
            } else if (zoom < 0.75f) {
                onZoomChange(-1) // zoom out
                lastGestureTime = now
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(pinchZoomModifier)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dedicated controls layout inside the screen for gorgeous scannability
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = query,
                    onValueChange = { onQueryChange(it) },
                    placeholder = { Text("Search title, description...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Secondary Controls Row: Date Picker, Display type, Filter Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date pick picker trigger
                    IconButton(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val picked = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    val queryStr = DateTimeUtils.formatDbDate(picked)
                                    onQueryChange(queryStr) // Filter by picked date
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Pick Date as Filter")
                    }

                    // Display Type Toggle (List or Container card groups)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        IconButton(
                            onClick = { onDisplayTypeChange(DisplayType.LIST) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (displayType == DisplayType.LIST) MaterialTheme.colorScheme.primaryContainer 
                                    else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "List display option",
                                tint = if (displayType == DisplayType.LIST) MaterialTheme.colorScheme.onPrimaryContainer 
                                       else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDisplayTypeChange(DisplayType.GROUPED) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (displayType == DisplayType.GROUPED) MaterialTheme.colorScheme.primaryContainer 
                                    else Color.Transparent
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grouped display option",
                                tint = if (displayType == DisplayType.GROUPED) MaterialTheme.colorScheme.onPrimaryContainer 
                                       else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Filter dropdown
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        listOf(
                            Pair(FilterOption.ALL, "All"),
                            Pair(FilterOption.MARKED_COMPLETE, "Done"),
                            Pair(FilterOption.LEFT_INCOMPLETE, "Pending")
                        ).forEach { (opt, label) ->
                            val active = activeFilter == opt
                            Box(
                                modifier = Modifier
                                    .clickable { onFilterChange(opt) }
                                    .background(
                                        if (active) MaterialTheme.colorScheme.secondaryContainer 
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                MainFontText(
                                    text = label,
                                    fontSize = AppFontSizes.extraSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer 
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Timeline Content List with AnimatedContent to handle cross-fades
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history matches your search filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            AnimatedContent(
                targetState = zoomLevel,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "HistoryZoomTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_tasks_list")
            ) { targetZoom ->
                when (targetZoom) {
                    0 -> YearView(tasks = tasks, colorSchemeType = colorSchemeType)
                    1 -> MonthView(tasks = tasks, colorSchemeType = colorSchemeType)
                    2 -> DayView(tasks = tasks, displayType = displayType, colorSchemeType = colorSchemeType)
                    3 -> ExpandedView(tasks = tasks, colorSchemeType = colorSchemeType)
                    else -> DayView(tasks = tasks, displayType = displayType, colorSchemeType = colorSchemeType)
                }
            }
        }
    }
}

/**
 * Composable text that uses App Font Sizes.
 */
@Composable
fun MainFontText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}

/**
 * Level 0: Year View (Completed Tasks only, represented by priority blocks)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YearView(
    tasks: List<Task>,
    colorSchemeType: String
) {
    val completedTasks = remember(tasks) { tasks.filter { it.isCompleted } }
    
    val groupedByYear = remember(completedTasks) {
        completedTasks.groupBy { task ->
            val date = DateTimeUtils.parseDbDate(task.dateAdded)
            val cal = Calendar.getInstance()
            if (date != null) cal.time = date
            cal.get(Calendar.YEAR).toString()
        }.toSortedMap(compareByDescending { it })
    }
    
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (completedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No completed tasks to display in Year View",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            groupedByYear.forEach { (year, yearTasks) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (colorSchemeType) {
                            "simple" -> MaterialTheme.colorScheme.surface
                            "minimal" -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        }
                    ),
                    border = when (colorSchemeType) {
                        "simple" -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        "minimal" -> {
                            val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                            androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
                        }
                        else -> null
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = year,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            yearTasks.forEach { task ->
                                val containerCol = getPriorityBoxColor(task.priority, isCompleted = false)
                                val isSelected = selectedTask?.id == task.id
                                
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(containerCol)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            selectedTask = if (isSelected) null else task
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Drawer-like task details at the bottom of the scroll view
        selectedTask?.let { task ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Task Details",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { selectedTask = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close details",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (task.description.isNotBlank()) {
                        Text(
                            text = parseStyledDescription(task.description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Priority: ${task.priority}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Date: ${task.dateAdded}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Level 1: Month View (Groups tasks by Month, showing medium text snippets)
 */
@Composable
fun MonthView(
    tasks: List<Task>,
    colorSchemeType: String
) {
    val groupedByMonth = remember(tasks) {
        tasks.groupBy { task ->
            val date = DateTimeUtils.parseDbDate(task.dateAdded)
            val cal = Calendar.getInstance()
            if (date != null) cal.time = date
            DateTimeUtils.formatMonthYear(cal.time)
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        groupedByMonth.forEach { (monthStr, monthTasks) ->
            item(key = monthStr) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (colorSchemeType) {
                            "simple" -> MaterialTheme.colorScheme.surface
                            "minimal" -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        }
                    ),
                    border = when (colorSchemeType) {
                        "simple" -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        "minimal" -> {
                            val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                            androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
                        }
                        else -> null
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = monthStr,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        
                        monthTasks.forEach { task ->
                            MediumTaskRow(task = task, colorSchemeType = colorSchemeType)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediumTaskRow(task: Task, colorSchemeType: String) {
    val isCompleted = task.isCompleted
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) MaterialTheme.colorScheme.primaryContainer 
                    else Color.Transparent
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontWeight = FontWeight.Bold,
                fontSize = AppFontSizes.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
            
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    fontSize = AppFontSizes.extraSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Level 2: Day View (Default - Groups tasks by exact date, standard detailed items)
 */
@Composable
fun DayView(
    tasks: List<Task>,
    displayType: DisplayType,
    colorSchemeType: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (displayType == DisplayType.GROUPED) {
            val grouped = tasks.groupBy { it.dateAdded }
            items(grouped.keys.toList().sortedDescending()) { dateStr ->
                val dateObj = DateTimeUtils.parseDbDate(dateStr) ?: Date()
                val groupTasks = grouped[dateStr] ?: emptyList()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (colorSchemeType) {
                            "simple" -> MaterialTheme.colorScheme.surface
                            "minimal" -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        }
                    ),
                    border = when (colorSchemeType) {
                        "simple" -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        "minimal" -> {
                            val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                            androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
                        }
                        else -> null
                    },
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = DateTimeUtils.formatHistoryGroup(dateObj),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${groupTasks.size} tasks",
                                    fontSize = AppFontSizes.nano,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                        groupTasks.forEach { task ->
                            ZoomableTaskRow(task, zoomLevel = 2, colorSchemeType = colorSchemeType)
                        }
                    }
                }
            }
        } else {
            items(tasks) { task ->
                ZoomableTaskRow(task, zoomLevel = 2, colorSchemeType = colorSchemeType)
            }
        }
    }
}

/**
 * Level 3: Expanded View (Groups tasks by exact date, shows continuous vertical details stream)
 */
@Composable
fun ExpandedView(
    tasks: List<Task>,
    colorSchemeType: String
) {
    val grouped = remember(tasks) { tasks.groupBy { it.dateAdded } }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        grouped.keys.toList().sortedDescending().forEach { dateStr ->
            val dateObj = DateTimeUtils.parseDbDate(dateStr) ?: Date()
            val groupTasks = grouped[dateStr] ?: emptyList()
            
            item(key = dateStr) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (colorSchemeType) {
                            "simple" -> MaterialTheme.colorScheme.surface
                            "minimal" -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        }
                    ),
                    border = when (colorSchemeType) {
                        "simple" -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                        "minimal" -> {
                            val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                            androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
                        }
                        else -> null
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = DateTimeUtils.formatHistoryGroup(dateObj),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        
                        groupTasks.forEach { task ->
                            ExpandedTaskRow(task = task, colorSchemeType = colorSchemeType)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandedTaskRow(task: Task, colorSchemeType: String) {
    val isCompleted = task.isCompleted
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCompleted) 0.65f else 1.0f),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                if (colorSchemeType == "minimal") {
                    if (isDark) Color(0x15FFFFFF) else Color(0x33B0AAB9)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                }
            } else {
                if (colorSchemeType == "minimal") {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            }
        ),
        border = if (colorSchemeType == "minimal") {
            if (isCompleted) {
                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder)
            }
        } else {
            null
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.primaryContainer 
                            else Color.Transparent
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = AppFontSizes.large,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
                
                val badgeStyle = if (colorSchemeType == "minimal") {
                    getMinimalPriorityColors(task.priority, isCompleted, isDark)
                } else {
                    val containerCol = getPriorityBoxColor(task.priority, isCompleted)
                    val textCol = if (task.priority == 4 || isCompleted) Color.DarkGray else Color.White
                    PriorityThemeBadgeColors(containerCol, textCol, Color.Transparent)
                }
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeStyle.containerColor)
                        .then(
                            if (colorSchemeType == "minimal") {
                                Modifier.border(1.dp, badgeStyle.borderColor, RoundedCornerShape(6.dp))
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = task.priority.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = AppFontSizes.micro,
                        color = badgeStyle.contentColor
                    )
                }
            }
            
            if (task.description.isNotBlank()) {
                Text(
                    text = parseStyledDescription(task.description),
                    fontSize = AppFontSizes.medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (task.reminderTime != null || task.isRecurring) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.reminderTime != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Active Reminder",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = DateTimeUtils.formatAlarmTime(task.reminderTime),
                                fontSize = AppFontSizes.extraSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    if (task.isRecurring) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Recurring Task",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Everyday",
                                fontSize = AppFontSizes.extraSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zoomable item representing a row. Spacings and fonts respond to zoom levels.
 */
@Composable
fun ZoomableTaskRow(task: Task, zoomLevel: Int, colorSchemeType: String) {
    val isCompleted = task.isCompleted
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    // Zoom dynamic attributes mapping
    val paddingValues = when (zoomLevel) {
        1 -> PaddingValues(vertical = 4.dp, horizontal = 8.dp)
        2 -> PaddingValues(vertical = 6.dp, horizontal = 10.dp)
        3 -> PaddingValues(vertical = 10.dp, horizontal = 12.dp)
        else -> PaddingValues(10.dp)
    }

    val titleSize = AppFontSizes.titleForZoom(zoomLevel)
    val bodySize = AppFontSizes.bodyForZoom(zoomLevel)

    val cardShape = when (zoomLevel) {
        1 -> RoundedCornerShape(6.dp)
        2 -> RoundedCornerShape(10.dp)
        else -> RoundedCornerShape(14.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isCompleted) 0.55f else 1.0f),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                if (colorSchemeType == "minimal") {
                    if (isDark) Color(0x15FFFFFF) else Color(0x33B0AAB9)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                }
            } else {
                if (colorSchemeType == "minimal") {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            }
        ),
        border = if (colorSchemeType == "minimal") {
            if (isCompleted) {
                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x11FFFFFF) else Color(0x33E2E8F0))
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder)
            }
        } else {
            null
        },
        shape = cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Elegant circular checkbox structure
            Box(
                modifier = Modifier
                    .size(if (zoomLevel == 1) 14.dp else 18.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                    .then(
                        if (!isCompleted) {
                            Modifier.border(
                                1.5.dp,
                                MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(if (zoomLevel == 1) 9.dp else 11.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = titleSize,
                        maxLines = if (zoomLevel == 1) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    // Display tiny date if not in Grouped mode
                    Text(
                        text = "• " + task.dateAdded,
                        fontSize = if (zoomLevel == 1) AppFontSizes.pico else AppFontSizes.micro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Show descriptions if user is not fully zoomed out
                if (zoomLevel > 1 && task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parseStyledDescription(task.description),
                        fontSize = bodySize,
                        maxLines = if (zoomLevel == 2) 1 else 4,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Small priority box matching the Home design
            val badgeStyle = if (colorSchemeType == "minimal") {
                getMinimalPriorityColors(task.priority, isCompleted, isDark)
            } else {
                val containerCol = getPriorityBoxColor(task.priority, isCompleted)
                val textCol = if (task.priority == 4 || isCompleted) Color.DarkGray else Color.White
                val borderCol = Color.Transparent
                PriorityThemeBadgeColors(containerCol, textCol, borderCol)
            }

            Box(
                modifier = Modifier
                    .size(if (zoomLevel == 1) 16.dp else 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeStyle.containerColor)
                    .then(
                        if (colorSchemeType == "minimal") {
                            Modifier.border(1.dp, badgeStyle.borderColor, RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = task.priority.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (zoomLevel == 1) AppFontSizes.pico else AppFontSizes.micro,
                    color = badgeStyle.contentColor
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "History Screen - Colorful Theme")
@Composable
fun HistoryScreenPreview() {
    SoftTodoTheme(colorSchemeType = "colorful") {
        HistoryScreenContent(
            tasks = sampleHistoryTasks,
            query = "",
            zoomLevel = 2,
            displayType = DisplayType.GROUPED,
            activeFilter = FilterOption.ALL,
            colorSchemeType = "colorful",
            onQueryChange = {},
            onZoomChange = {},
            onDisplayTypeChange = {},
            onFilterChange = {}
        )
    }
}

@Preview(showBackground = true, name = "Zoomable Task Row")
@Composable
fun ZoomableTaskRowPreview() {
    SoftTodoTheme {
        ZoomableTaskRow(
            task = sampleHistoryTasks[0],
            zoomLevel = 3,
            colorSchemeType = "minimal"
        )
    }
}

private val sampleHistoryTasks = listOf(
    Task(id = 1, title = "Finish Project Proposal", description = "Finalize the budget and timeline", priority = 1, dateAdded = "2023-10-25", isCompleted = true),
    Task(id = 2, title = "Grocery Shopping", description = "Milk, Eggs, Bread, Fruits", priority = 2, dateAdded = "2023-10-25", isCompleted = true),
    Task(id = 3, title = "Gym Workout", description = "Leg day", priority = 3, dateAdded = "2023-10-24", isCompleted = false)
)
