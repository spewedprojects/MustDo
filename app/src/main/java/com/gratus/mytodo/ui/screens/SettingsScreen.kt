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

package com.gratus.mytodo.ui.screens

import android.media.RingtoneManager
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.components.settings.AestheticsSettingsCard
import com.gratus.mytodo.ui.components.settings.BackupsRestorationsCard
import com.gratus.mytodo.ui.components.settings.FeaturePreferencesCard
import com.gratus.mytodo.ui.components.settings.ReminderSettingsCard
import com.gratus.mytodo.ui.theme.SoftTodoTheme

/**
 * SettingsScreen includes theme configurations, color scheme selectors, and backups.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    colorSchemeType: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val activeTheme by viewModel.settingsTheme.collectAsState()
    val activeScheme by viewModel.settingsColorScheme.collectAsState()
    val activeInterval by viewModel.settingsReminderInterval.collectAsState()
    val isAlarmGranted by viewModel.isAlarmPermissionGranted.collectAsState()
    val isNotificationGranted by viewModel.isNotificationPermissionGranted.collectAsState()
    val isFullScreenGranted by viewModel.isFullScreenPermissionGranted.collectAsState()
    val alarmRingtoneUri by viewModel.settingsAlarmRingtone.collectAsState()
    val colorfulHueShift by viewModel.colorfulHueShift.collectAsState()
    val colorfulSatScale by viewModel.colorfulSatScale.collectAsState()

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.setAlarmRingtone(uri?.toString())
        }
    }

    val isStickyEnabled by viewModel.isStickyEnabled.collectAsState()

    SettingsScreenContent(
        activeTheme = activeTheme,
        activeScheme = activeScheme,
        activeInterval = activeInterval,
        isAlarmPermissionGranted = isAlarmGranted,
        isNotificationPermissionGranted = isNotificationGranted,
        isFullScreenPermissionGranted = isFullScreenGranted,
        colorfulHueShift = colorfulHueShift,
        colorfulSatScale = colorfulSatScale,
        isStickyEnabled = isStickyEnabled,
        onThemeChange = { viewModel.setTheme(it) },
        onSchemeChange = { viewModel.setColorScheme(it) },
        onIntervalChange = { viewModel.setReminderInterval(it) },
        onHueShiftChange = { viewModel.setColorfulHueShift(it) },
        onSatScaleChange = { viewModel.setColorfulSatScale(it) },
        onStickyEnabledChange = { viewModel.setStickyEnabled(it) },
        onExportJson = { outputStream ->
            try {
                val json = viewModel.exportBackup()
                if (json.isNotBlank()) {
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        },
        onExportDb = { outputStream ->
            try {
                viewModel.checkpointDatabase()
                val dbFile = viewModel.getApplication<android.app.Application>().getDatabasePath("task_database")
                dbFile.inputStream().use { input ->
                    input.copyTo(outputStream)
                }
                true
            } catch (e: Exception) {
                false
            }
        },
        onImportBackup = { uri, onComplete ->
            viewModel.importBackupUri(uri) { success, isDb ->
                onComplete(success, isDb)
            }
        },
        ringtoneUri = alarmRingtoneUri,
        onRingtoneClick = {
            val existingUri = alarmRingtoneUri?.let { Uri.parse(it) }
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Tone")
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            }
            ringtonePickerLauncher.launch(intent)
        }
    )
}

@Composable
fun SettingsScreenContent(
    activeTheme: String,
    activeScheme: String,
    activeInterval: Int,
    isAlarmPermissionGranted: Boolean,
    isNotificationPermissionGranted: Boolean,
    isFullScreenPermissionGranted: Boolean = true,
    colorfulHueShift: Float = 0f,
    colorfulSatScale: Float = 1f,
    isStickyEnabled: Boolean = true,
    onThemeChange: (String) -> Unit = {},
    onSchemeChange: (String) -> Unit = {},
    onIntervalChange: (Int) -> Unit = {},
    onHueShiftChange: (Float) -> Unit = {},
    onSatScaleChange: (Float) -> Unit = {},
    onStickyEnabledChange: (Boolean) -> Unit = {},
    onExportJson: (java.io.OutputStream) -> Boolean = { false },
    onExportDb: (java.io.OutputStream) -> Boolean = { false },
    onImportBackup: (Uri, (Boolean, Boolean) -> Unit) -> Unit = { _, _ -> },
    ringtoneUri: String?,
    onRingtoneClick: () -> Unit
) {
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onImportBackup(uri) { success, isDb ->
                if (success) {
                    if (isDb) {
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            putExtra("SHOW_RESTORE_SUCCESS_TOAST", true)
                        }
                        if (intent != null) {
                            context.startActivity(intent)
                        }
                    } else {
                        Toast.makeText(context, "JSON Backup imported successfully! Alarms recalculated.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Import failed: Invalid backup file format", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Preferences Container (Sticky Tasks Toggle)
        FeaturePreferencesCard(
            isStickyEnabled = isStickyEnabled,
            onStickyEnabledChange = onStickyEnabledChange
        )

        // Theme & Scheme Configuration Container
        AestheticsSettingsCard(
            activeTheme = activeTheme,
            activeScheme = activeScheme,
            colorfulHueShift = colorfulHueShift,
            colorfulSatScale = colorfulSatScale,
            onThemeChange = onThemeChange,
            onSchemeChange = onSchemeChange,
            onHueShiftChange = onHueShiftChange,
            onSatScaleChange = onSatScaleChange
        )

        // Reminder Settings Container
        ReminderSettingsCard(
            isAlarmPermissionGranted = isAlarmPermissionGranted,
            isNotificationPermissionGranted = isNotificationPermissionGranted,
            isFullScreenPermissionGranted = isFullScreenPermissionGranted,
            activeInterval = activeInterval,
            ringtoneUri = ringtoneUri,
            context = context,
            onIntervalChange = onIntervalChange,
            onRingtoneClick = onRingtoneClick
        )

        // Backups & Exports Container
        BackupsRestorationsCard(
            context = context,
            importLauncher = importLauncher,
            onExportJson = onExportJson,
            onExportDb = onExportDb
        )
    }
}

@Preview(showBackground = true, heightDp = 1800, name = "Settings Screen - Scrollable Light")
@Composable
fun SettingsScreenPreview() {
    SoftTodoTheme {
        SettingsScreenContent(
            activeTheme = "light",
            activeScheme = "minimal",
            activeInterval = 10,
            isAlarmPermissionGranted = true,
            isNotificationPermissionGranted = true,
            onThemeChange = {},
            onSchemeChange = {},
            onIntervalChange = {},
            onExportJson = { true },
            onExportDb = { true },
            onImportBackup = { _, _ -> },
            ringtoneUri = null,
            onRingtoneClick = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Settings Screen - Navigable Dark Mode")
@Composable
fun SettingsScreenNavigableDarkPreview() {
    SoftTodoTheme {
        SettingsScreenContent(
            activeTheme = "dark",
            activeScheme = "minimal",
            activeInterval = 10,
            isAlarmPermissionGranted = true,
            isNotificationPermissionGranted = true,
            onThemeChange = {},
            onSchemeChange = {},
            onIntervalChange = {},
            onExportJson = { true },
            onExportDb = { true },
            onImportBackup = { _, _ -> },
            ringtoneUri = null,
            onRingtoneClick = {}
        )
    }
}
