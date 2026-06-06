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
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.DisplayType
import com.gratus.mytodo.ui.FilterOption
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Historical records screen with filtering, date classification, visual zoom, and pinch-to-zoom.
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
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val titleSdf = remember { SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()) }

    // State to throttling scale gestures
    var lastGestureTime by remember { mutableLongStateOf(0L) }

    // Pinch to Zoom math utilizing pointers transform detector
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

                // Secondary Controls Row: Date Picker, Display type, Filter Type, Zoom Level
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
                                    val queryStr = sdf.format(picked.time)
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
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer 
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Zoom level controls (+ / -)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onZoomChange(-1) },
                            enabled = zoomLevel > 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                        }
                        
                        Text(
                            text = "x$zoomLevel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { onZoomChange(1) },
                            enabled = zoomLevel < 5,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Timeline Content List
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_tasks_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (displayType == DisplayType.GROUPED) {
                    // Group tasks by their added date
                    val grouped = tasks.groupBy { it.dateAdded }
                    items(grouped.keys.toList().sortedDescending()) { dateStr ->
                        val dateObj = sdf.parse(dateStr) ?: Date()
                        val groupTasks = grouped[dateStr] ?: emptyList()

                        // Each group enclosed in its own modern rounded Card container
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_container_$dateStr"),
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
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Header for the card container group
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = titleSdf.format(dateObj),
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
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                groupTasks.forEach { task ->
                                    ZoomableTaskRow(task, zoomLevel, colorSchemeType)
                                }
                            }
                        }
                    }
                } else {
                    // Standard continuous List display
                    items(tasks) { task ->
                        ZoomableTaskRow(task, zoomLevel, colorSchemeType)
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
        4 -> PaddingValues(vertical = 14.dp, horizontal = 16.dp)
        5 -> PaddingValues(vertical = 18.dp, horizontal = 18.dp)
        else -> PaddingValues(10.dp)
    }

    val titleSize = when (zoomLevel) {
        1 -> 11.sp
        2 -> 13.sp
        3 -> 15.sp
        4 -> 17.sp
        5 -> 19.sp
        else -> 15.sp
    }

    val bodySize = when (zoomLevel) {
        1 -> 9.sp
        3 -> 11.sp
        4 -> 13.sp
        5 -> 14.sp
        else -> 11.sp
    }

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
            // Elegant square checkbox structure for minimal scheme, standard check icon otherwise
            Box(
                modifier = Modifier
                    .size(if (zoomLevel == 1) 14.dp else 18.dp)
                    .clip(if (colorSchemeType == "minimal") RoundedCornerShape(4.dp) else CircleShape)
                    .background(
                        if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                    )
                    .then(
                        if (!isCompleted) {
                            Modifier.border(
                                1.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                if (colorSchemeType == "minimal") RoundedCornerShape(4.dp) else CircleShape
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
                        tint = Color.White,
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
                        fontSize = if (zoomLevel == 1) 8.sp else 10.sp,
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
                    fontSize = if (zoomLevel == 1) 8.sp else 10.sp,
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
