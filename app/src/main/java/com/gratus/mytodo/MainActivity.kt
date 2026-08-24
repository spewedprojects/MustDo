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

import com.gratus.mytodo.ui.components.navigation.AppDrawerContent
import com.gratus.mytodo.ui.components.navigation.MainLayoutContent

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.saveable.rememberSaveable
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
        viewModel.checkPermissions(this)
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

        handleIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_OPEN_ADD_TASK, false) || intent.action == ACTION_ADD_TASK) {
            viewModel.setActiveScreen(Screen.HOME)
            viewModel.setShowAddDialog(true)
        }
    }

    companion object {
        const val ACTION_ADD_TASK = "com.gratus.mytodo.action.ADD_TASK"
        const val EXTRA_OPEN_ADD_TASK = "open_add_task"
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
        onSetDate = { viewModel.setDate(it) },
        onToggleSorting = {
            viewModel.toggleSorting()
            val label = if (sortOption == SortOption.PRIORITY) "Sequence chronological" else "Priority list levels"
            Toast.makeText(viewModel.getApplication(), "Sorted by $label", Toast.LENGTH_SHORT).show()
        },
        screenContent = { onOpenDrawer, isInlineCalendarExpanded, onToggleInlineCalendar ->
            when (activeScreen) {
                Screen.HOME -> HomeScreen(
                    viewModel = viewModel,
                    onOpenDrawer = onOpenDrawer,
                    colorSchemeType = colorSchemeType,
                    isInlineCalendarExpanded = isInlineCalendarExpanded,
                    onToggleInlineCalendar = onToggleInlineCalendar
                )
                Screen.HISTORY  -> HistoryScreen(
                    viewModel = viewModel,
                    colorSchemeType = colorSchemeType,
                    onNavigateToHomeDate = { targetCal, taskId ->
                        viewModel.setDate(targetCal)
                        if (taskId != null) {
                            viewModel.setHighlightedTaskId(taskId)
                        }
                        viewModel.setActiveScreen(Screen.HOME)
                    }
                )
                Screen.STATS    -> StatsScreen(viewModel, colorSchemeType)
                Screen.SETTINGS -> SettingsScreen(viewModel, colorSchemeType)
            }
        }
    )
}
