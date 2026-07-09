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

package com.gratus.mytodo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.Screen
import com.gratus.mytodo.ui.SortOption
import com.gratus.mytodo.ui.components.FaintBackground
import com.gratus.mytodo.ui.screens.HistoryScreen
import com.gratus.mytodo.ui.screens.HomeScreen
import com.gratus.mytodo.ui.screens.IssueTrackerScreen
import com.gratus.mytodo.ui.screens.SettingsScreen
import com.gratus.mytodo.ui.screens.StatsScreen
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import kotlinx.coroutines.launch
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.*
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.automirrored.filled.Sort

/**
 * MainActivity is the host core of the Soft To-Do application.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Activity launcher for runtime permissions (Android 13+)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Reminders won't show notifications without permissions", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if database was restored successfully to show the toast
        if (intent.getBooleanExtra("SHOW_RESTORE_SUCCESS_TOAST", false)) {
            Toast.makeText(this, "Database restored successfully!", Toast.LENGTH_LONG).show()
        }

        // Prompt notification permission at boot on Android 13+
        checkNotificationPermissions()

        setContent {
            val activeTheme by viewModel.settingsTheme.collectAsState()
            val colorSchemeType by viewModel.settingsColorScheme.collectAsState()
            val colorfulHueShift by viewModel.colorfulHueShift.collectAsState()
            val colorfulSatScale by viewModel.colorfulSatScale.collectAsState()

            val isDark = when (activeTheme) {
                "light" -> false
                "dark"  -> true
                else    -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            SoftTodoTheme(
                themeMode = activeTheme,
                colorSchemeType = colorSchemeType,
                colorfulHueShift = colorfulHueShift,
                colorfulSatScale = colorfulSatScale
            ) {
                MainLayout(viewModel, colorSchemeType, isDark, colorfulHueShift, colorfulSatScale)
            }
        }
    }

    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions(this)
    }
}

/**
 * MainLayout containing Drawer, Persistent Header, and the Dynamic body.
 */
@Composable
fun MainLayout(
    viewModel: MainViewModel,
    colorSchemeType: String,
    isDark: Boolean,
    colorfulHueShift: Float,
    colorfulSatScale: Float
) {
    val activeScreen by viewModel.activeScreen.collectAsState()
    val focusDate by viewModel.currentDate.collectAsState()
    val sortOption by viewModel.sortingOption.collectAsState()

    MainLayoutContent(
        activeScreen = activeScreen,
        focusDate = focusDate,
        sortOption = sortOption,
        colorSchemeType = colorSchemeType,
        isDark = isDark,
        colorfulHueShift = colorfulHueShift,
        colorfulSatScale = colorfulSatScale,
        onSetActiveScreen = { viewModel.setActiveScreen(it) },
        onNavigateDate = { viewModel.navigateDate(it) },
        onToggleSorting = {
            viewModel.toggleSorting()
            val label = if (sortOption == SortOption.PRIORITY) "Sequence chronological" else "Priority list levels"
            Toast.makeText(viewModel.getApplication(), "Sorted by $label", Toast.LENGTH_SHORT).show()
        },
        screenContent = { onOpenDrawer ->
            when (activeScreen) {
                Screen.HOME     -> HomeScreen(viewModel, onOpenDrawer, colorSchemeType)
                Screen.HISTORY  -> HistoryScreen(viewModel, colorSchemeType)
                Screen.STATS    -> StatsScreen(viewModel, colorSchemeType)
                Screen.SETTINGS -> SettingsScreen(viewModel, colorSchemeType)
                Screen.ISSUE_TRACKER -> IssueTrackerScreen(onOpenDrawer, colorSchemeType)
            }
        }
    )
}

/**
 * Stateless version of MainLayout for preview and testing.
 */
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
    onToggleSorting: () -> Unit,
    screenContent: @Composable (onOpenDrawer: () -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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
                    .windowInsetsPadding(WindowInsets.statusBars), // Safely pad camera notch / cutout
                containerColor = Color.Transparent, // Let the custom backgrounds flow through
                topBar = {
                        CenterAlignedTopAppBar(
                        title = {
                            if (activeScreen == Screen.HOME) {
                                // Center consumed by Date and arrows to navigate dates
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Faint Left Date Increment
                                    IconButton(
                                        onClick = { onNavigateDate(-1) },
                                        modifier = Modifier.alpha(0.6f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowLeft,
                                            contentDescription = "Previous Day",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    val headerText = when {
                                        DateTimeUtils.isToday(focusDate) -> "Today"
                                        DateTimeUtils.isYesterday(focusDate) -> "Yesterday"
                                        DateTimeUtils.isTomorrow(focusDate) -> "Tomorrow"
                                        else -> DateTimeUtils.formatMainHeader(focusDate)
                                    }
                                    Text(
                                        text = headerText,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Faint Right Date Increment
                                    IconButton(
                                        onClick = { onNavigateDate(1) },
                                        modifier = Modifier.alpha(0.6f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Next Day",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                // Center displays modern static names for persistent alignment
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
                            // Left hamburger menu icon to reveal drawer
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
                            // Custom Action based on active screen
                            if (activeScreen == Screen.HOME) {
                                // Sorting trigger button (Priority order vs Chronological Addition Sequence)
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
                                // Keep slot padding consistent for visual balance
                                Box(modifier = Modifier.size(48.dp))
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent, // Keep transparent so backdrops show clean
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
                    screenContent { coroutineScope.launch { drawerState.open() } }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerContent(
    activeScreen: Screen,
    colorSchemeType: String,
    onSetActiveScreen: (Screen) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = Modifier
            .width(300.dp)
            .testTag("nav_drawer_sheet"),
        drawerShape = RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp),
        drawerContainerColor = if (colorSchemeType == "minimal" || colorSchemeType == "colorful") {
            MaterialTheme.colorScheme.background
        } else {
            MaterialTheme.colorScheme.surface
        },
        drawerTonalElevation = 0.dp
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Branding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "MustDo",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (0.5).sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Time-locked task tracker",
                fontSize = AppFontSizes.extraSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Items
        val navItems = listOf(
            Triple(Screen.HOME, "Home", Icons.Default.Home),
            Triple(Screen.HISTORY, "History Records", Icons.Default.History),
            Triple(Screen.STATS, "Stats Analyzer", Icons.Default.Analytics),
            Triple(Screen.SETTINGS, "App Settings", Icons.Default.Settings)
        )

        navItems.forEach { (screenKey, name, icon) ->
            if (screenKey == Screen.SETTINGS) {
                val selected = activeScreen == screenKey
                val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .height(56.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .combinedClickable(
                            onClick = {
                                onSetActiveScreen(screenKey)
                                onCloseDrawer()
                            },
                            onLongClick = {
                                onSetActiveScreen(Screen.ISSUE_TRACKER)
                                onCloseDrawer()
                            }
                        ),
                    color = containerColor,
                    contentColor = contentColor,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (selected) MaterialTheme.colorScheme.primary else contentColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = AppFontSizes.medium,
                            color = contentColor
                        )
                    }
                }
            } else {
                NavigationDrawerItem(
                    icon = { Icon(imageVector = icon, contentDescription = name) },
                    label = { Text(name, fontWeight = FontWeight.Bold, fontSize = AppFontSizes.medium) },
                    selected = activeScreen == screenKey,
                    onClick = {
                        onSetActiveScreen(screenKey)
                        onCloseDrawer()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // GitHub Button
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW,"https://github.com/spewedprojects/MustDo".toUri())
                context.startActivity(intent)
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.github_mark),
                contentDescription = "Star on GitHub",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp) // Adjusted to a more standard size
            )
        }

        // Version Info
        Text(
            text = "v${BuildConfig.VERSION_NAME}" + " (${BuildConfig.VERSION_CODE})",
            fontSize = AppFontSizes.micro,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainLayoutPreview() {
    SoftTodoTheme {
        MainLayoutContent(
            activeScreen = Screen.HOME,
            focusDate = Calendar.getInstance(),
            sortOption = SortOption.PRIORITY,
            colorSchemeType = "minimal",
            onSetActiveScreen = {},
            onNavigateDate = {},
            onToggleSorting = {},
            screenContent = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Main Layout Preview Content")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NavDrawerPreview() {
    SoftTodoTheme {
        // We call the shared content directly
        AppDrawerContent(
            activeScreen = Screen.HOME,
            colorSchemeType = "minimal",
            onSetActiveScreen = {},
            onCloseDrawer = {}
        )
    }
}
