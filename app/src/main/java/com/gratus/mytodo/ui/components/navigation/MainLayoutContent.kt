package com.gratus.mytodo.ui.components.navigation

import com.gratus.mytodo.ui.components.FaintBackground

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.Screen
import com.gratus.mytodo.ui.SortOption
import com.gratus.mytodo.ui.components.FaintBackground
import com.gratus.mytodo.ui.utils.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayoutContent(
    activeScreen: Screen,
    focusDate: Calendar,
    sortOption: SortOption,
    colorSchemeType: String,
    isDark: Boolean = false,
    colorfulHueShift: Float = 0f,
    colorfulSatScale: Float = 1f,
    onSetActiveScreen: (Screen) -> Unit,
    onNavigateDate: (Int) -> Unit,
    onSetDate: (Calendar) -> Unit = {},
    onToggleSorting: () -> Unit,
    screenContent: @Composable (onOpenDrawer: () -> Unit, isInlineCalendarExpanded: Boolean, onToggleInlineCalendar: () -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isInlineCalendarExpanded by rememberSaveable { mutableStateOf(false) }

    if (activeScreen != Screen.HOME) {
        androidx.activity.compose.BackHandler {
            onSetActiveScreen(Screen.HOME)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                activeScreen = activeScreen,
                colorSchemeType = colorSchemeType,
                onSetActiveScreen = onSetActiveScreen,
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        FaintBackground(
            colorSchemeType = colorSchemeType,
            isDark = isDark,
            colorfulHueShift = colorfulHueShift,
            colorfulSatScale = colorfulSatScale
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            if (activeScreen == Screen.HOME) {
                                val isToday = DateTimeUtils.isToday(focusDate)
                                val todayCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val focusCalNorm = Calendar.getInstance().apply {
                                    time = focusDate.time
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val isPast = focusCalNorm.before(todayCal)
                                val isFuture = focusCalNorm.after(todayCal)

                                val highlightLeft = !isToday && isFuture
                                val highlightRight = !isToday && isPast

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (highlightLeft) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = { onNavigateDate(-1) },
                                                    onLongPress = {
                                                        onSetDate(Calendar.getInstance())
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowLeft,
                                            contentDescription = "Previous Day (Long press to Jump to Today)",
                                            tint = if (highlightLeft) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val headerText = when {
                                        DateTimeUtils.isToday(focusDate) -> "Today"
                                        DateTimeUtils.isYesterday(focusDate) -> "Yesterday"
                                        DateTimeUtils.isTomorrow(focusDate) -> "Tomorrow"
                                        else -> DateTimeUtils.formatMainHeader(focusDate)
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { isInlineCalendarExpanded = !isInlineCalendarExpanded }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = headerText,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = if (isInlineCalendarExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle Calendar",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (highlightRight) MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent
                                            )
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = { onNavigateDate(1) },
                                                    onLongPress = {
                                                        onSetDate(Calendar.getInstance())
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Next Day (Long press to Jump to Today)",
                                            tint = if (highlightRight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = when (activeScreen) {
                                        Screen.HISTORY -> "Historical Timelines"
                                        Screen.STATS -> "Completion Statistics"
                                        Screen.SETTINGS -> "Settings Profile"
                                        Screen.ISSUE_TRACKER -> "Issue Tracker"
                                        else -> "MustDo"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("hamburger_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Drawer Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            if (activeScreen == Screen.HOME) {
                                IconButton(
                                    onClick = onToggleSorting,
                                    modifier = Modifier.testTag("sort_tasks_button")
                                ) {
                                    Icon(
                                        imageVector = if (sortOption == SortOption.PRIORITY) Icons.Default.LowPriority else Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Toggle Sort Mode",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.size(48.dp))
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    screenContent(
                        { coroutineScope.launch { drawerState.open() } },
                        isInlineCalendarExpanded,
                        { isInlineCalendarExpanded = !isInlineCalendarExpanded }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Main Layout - Navigable Home Screen")
@Composable
fun MainLayoutContentNavigableHomePreview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
        MainLayoutContent(
            activeScreen = Screen.HOME,
            focusDate = Calendar.getInstance(),
            sortOption = SortOption.PRIORITY,
            colorSchemeType = "minimal",
            onSetActiveScreen = {},
            onNavigateDate = {},
            onToggleSorting = {},
            screenContent = { _, _, _ ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Home Screen Content Area")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Main Layout - Navigable History Screen")
@Composable
fun MainLayoutContentNavigableHistoryPreview() {
    SoftTodoTheme(colorSchemeType = "minimal") {
        MainLayoutContent(
            activeScreen = Screen.HISTORY,
            focusDate = Calendar.getInstance(),
            sortOption = SortOption.PRIORITY,
            colorSchemeType = "minimal",
            onSetActiveScreen = {},
            onNavigateDate = {},
            onToggleSorting = {},
            screenContent = { _, _, _ ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("History Screen Content Area")
                }
            }
        )
    }
}
