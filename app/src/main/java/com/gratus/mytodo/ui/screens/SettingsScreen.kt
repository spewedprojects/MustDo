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

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.AppFontSizes
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.format

/**
 * SettingsScreen includes theme configurations, color scheme selectors, and backups.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    colorSchemeType: String
) {
    val activeTheme by viewModel.settingsTheme.collectAsState()
    val activeScheme by viewModel.settingsColorScheme.collectAsState()
    val activeInterval by viewModel.settingsReminderInterval.collectAsState()
    val isAlarmGranted by viewModel.isAlarmPermissionGranted.collectAsState()
    val isNotificationGranted by viewModel.isNotificationPermissionGranted.collectAsState()
    val alarmRingtoneUri by viewModel.settingsAlarmRingtone.collectAsState()

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            viewModel.setAlarmRingtone(uri?.toString())
        }
    }

    SettingsScreenContent(
        activeTheme = activeTheme,
        activeScheme = activeScheme,
        activeInterval = activeInterval,
        isAlarmPermissionGranted = isAlarmGranted,
        isNotificationPermissionGranted = isNotificationGranted,
        onThemeChange = { viewModel.setTheme(it) },
        onSchemeChange = { viewModel.setColorScheme(it) },
        onIntervalChange = { viewModel.setReminderInterval(it) },
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
    onThemeChange: (String) -> Unit,
    onSchemeChange: (String) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onExportJson: (java.io.OutputStream) -> Boolean,
    onExportDb: (java.io.OutputStream) -> Boolean,
    onImportBackup: (Uri, (Boolean, isDb: Boolean) -> Unit) -> Unit,
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
        // Theme & Scheme Configuration Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Aesthetics Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Light / Dark / Auto selectors
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themeList = listOf(
                            Pair("auto", "System Auto"),
                            Pair("light", "Light"),
                            Pair("dark", "Dark")
                        )

                        themeList.forEach { (mode, label) ->
                            val isSelected = activeTheme == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onThemeChange(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = AppFontSizes.small,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Color schemes options (Simple, Colorful, Monet)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Color Schemes Palette", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    val schemes = listOf(
                        Triple("minimal", "Clean Minimalism", "Lavender backing with space-blurry spheres, sleek borders, and elegant state indicators."),
                        Triple("simple", "Simple B&W Only", "Black and white base, accents colored strictly around Priority levels."),
                        Triple("colorful", "Pastel Colorful", "Soft pastel layers with faint radial sweeping neon screen background."),
                        Triple("system", "System Monet", "Dynamic native Material You colors synched directly from Android 12+ wallpaper settings.")
                    )

                    schemes.forEach { (schemeKey, name, desc) ->
                        val isSelected = activeScheme == schemeKey
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.secondary 
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSchemeChange(schemeKey) }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = when (schemeKey) {
                                            "minimal" -> Icons.Default.Spa
                                            "simple" -> Icons.Default.BrightnessLow
                                            "colorful" -> Icons.Default.Palette
                                            else -> Icons.Default.SettingsSuggest
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = AppFontSizes.large,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = AppFontSizes.small,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            lineHeight = AppFontSizes.medium
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSchemeChange(schemeKey) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reminder Settings Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Reminder Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "System Permissions Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Alarms & Reminders Permission Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alarms & Reminders",
                            fontSize = AppFontSizes.medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isAlarmPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isAlarmPermissionGranted) "Active" else "Disabled",
                                color = if (isAlarmPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = AppFontSizes.extraSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Notification Permission Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifications",
                            fontSize = AppFontSizes.medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isNotificationPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isNotificationPermissionGranted) "Active" else "Disabled",
                                color = if (isNotificationPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = AppFontSizes.extraSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!isAlarmPermissionGranted || !isNotificationPermissionGranted) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Change in System Settings",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = AppFontSizes.small,
                            modifier = Modifier
                                .clickable {
                                    val intent = if (!isNotificationPermissionGranted) {
                                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                        }
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = android.net.Uri.fromParts("package", context.packageName, null)
                                            }
                                        } else {
                                            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.fromParts("package", context.packageName, null)
                                            }
                                        }
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Reminder Repeat Interval",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set how many minutes the alarm waits to repeat itself (applied to all repeat intervals).",
                        fontSize = AppFontSizes.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = AppFontSizes.medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val intervals = listOf(
                            Pair(5, "5m"),
                            Pair(10, "10m"),
                            Pair(15, "15m"),
                            Pair(30, "30m"),
                            Pair(60, "60m")
                        )

                        intervals.forEach { (minutes, label) ->
                            val isSelected = activeInterval == minutes
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onIntervalChange(minutes) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = AppFontSizes.small,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))

                // Alarm ringtone configuration row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onRingtoneClick() }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alarm Ringtone",
                            fontSize = AppFontSizes.medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = getRingtoneTitle(context, ringtoneUri),
                            fontSize = AppFontSizes.small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Select Alarm Tone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Backups & Exports Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Backups & Restorations",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Text(
                    text = "Import or export your list entries easily. Alarms will be rescheduled cleanly upon successful restore.",
                    fontSize = AppFontSizes.extraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                // Actions Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export to Device Button
                    Button(
                        onClick = {
                            val jsonSuccess = saveBackupToDocuments(
                                context,
                                "todo_backup",
                                ".json",
                                "application/json"
                            ) { output ->
                                onExportJson(output)
                            }

                            val dbSuccess = saveBackupToDocuments(
                                context,
                                "todo_backup",
                                ".db",
                                "application/octet-stream"
                            ) { output ->
                                onExportDb(output)
                            }
                            if (jsonSuccess && dbSuccess) {
                                Toast.makeText(
                                    context,
                                    "Backup files exported to Documents folder!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else if (jsonSuccess) {
                                Toast.makeText(
                                    context,
                                    "JSON exported, but Database file export failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (dbSuccess) {
                                Toast.makeText(
                                    context,
                                    "Database exported, but JSON backup failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Export failed. Please check storage.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_device_btn")
                    ) {
                        Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export to Device")
                    }

                    // Import & Restore Backup Button
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch("*/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_file_btn")
                    ) {
                        Icon(imageVector = Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import & Restore Backup")
                    }
                }
            }
        }
    }
}

/**
 * Saves a backup file directly to the device's public Documents directory.
 * Appends the current date in yyyy-MM-dd format to the filename.
 */
private fun saveBackupToDocuments(
    context: Context,
    baseFileName: String, // e.g., "todo_backup"
    extension: String,     // e.g., ".json"
    mimeType: String,
    dataWriter: (java.io.OutputStream) -> Boolean
): Boolean {
    // 1. Prepare the date-stamped filename
    val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val fullFileName = "${baseFileName}_$dateStamp$extension"

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Changed from DIRECTORY_DOWNLOADS to DIRECTORY_DOCUMENTS
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MustdoBackups")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
    } else {
        // Fallback for pre-Android 10 (Legacy Storage)
        @Suppress("DEPRECATION")
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS + "/MustdoBackups")
        if (!docsDir.exists() && !docsDir.mkdirs()) return false
        val file = File(docsDir, fullFileName)
        try {
            file.outputStream().use { return dataWriter(it) }
        } catch (e: Exception) {
            null
        }
    } ?: return false

    var success = false
    try {
        resolver.openOutputStream(uri)?.use { output ->
            success = dataWriter(output)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    } catch (e: Exception) {
        success = false
    }
    return success
}

private fun getRingtoneTitle(context: Context, uriString: String?): String {
    if (uriString.isNullOrEmpty()) return "Default Alarm Tone"
    return try {
        val uri = Uri.parse(uriString)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.getTitle(context) ?: "Unknown Tone"
    } catch (e: Exception) {
        "Default Alarm Tone"
    }
}

@Preview(showBackground = true)
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
