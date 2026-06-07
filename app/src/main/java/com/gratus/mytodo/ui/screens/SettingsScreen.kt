package com.gratus.mytodo.ui.screens

import android.content.ContentValues
import android.content.Context
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

    SettingsScreenContent(
        activeTheme = activeTheme,
        activeScheme = activeScheme,
        onThemeChange = { viewModel.setTheme(it) },
        onSchemeChange = { viewModel.setColorScheme(it) },
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
        onImportBackup = { inputStream, onComplete ->
            try {
                val fileBytes = inputStream.readBytes()
                val magicString = if (fileBytes.size >= 15) {
                    String(fileBytes, 0, 15, Charsets.US_ASCII)
                } else {
                    ""
                }
                
                if (magicString == "SQLite format 3") {
                    val byteStream = java.io.ByteArrayInputStream(fileBytes)
                    viewModel.importDbBackup(byteStream) { success ->
                        onComplete(success, true)
                    }
                } else {
                    val jsonStr = String(fileBytes, Charsets.UTF_8)
                    viewModel.importBackup(jsonStr) { success ->
                        onComplete(success, false)
                    }
                }
            } catch (e: Exception) {
                onComplete(false, false)
            }
        }
    )
}

@Composable
fun SettingsScreenContent(
    activeTheme: String,
    activeScheme: String,
    onThemeChange: (String) -> Unit,
    onSchemeChange: (String) -> Unit,
    onExportJson: (java.io.OutputStream) -> Boolean,
    onExportDb: (java.io.OutputStream) -> Boolean,
    onImportBackup: (java.io.InputStream, (Boolean, isDb: Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    onImportBackup(inputStream) { success, isDb ->
                        if (success) {
                            if (isDb) {
                                Toast.makeText(context, "Database restored successfully! Restarting...", Toast.LENGTH_LONG).show()
                                val activity = context as? android.app.Activity
                                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(intent)
                                activity?.finish()
                                Runtime.getRuntime().exit(0)
                            } else {
                                Toast.makeText(context, "JSON Backup imported successfully! Alarms recalculated.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Import failed: Invalid backup file format", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            val jsonSuccess = saveBackupToDownloads(context, "todo_backup.json", "application/json") { output ->
                                onExportJson(output)
                            }
                            val dbSuccess = saveBackupToDownloads(context, "todo_backup.db", "application/octet-stream") { output ->
                                onExportDb(output)
                            }
                            if (jsonSuccess && dbSuccess) {
                                Toast.makeText(context, "Backup files exported to Downloads folder!", Toast.LENGTH_LONG).show()
                            } else if (jsonSuccess) {
                                Toast.makeText(context, "JSON exported, but Database file export failed", Toast.LENGTH_SHORT).show()
                            } else if (dbSuccess) {
                                Toast.makeText(context, "Database exported, but JSON backup failed", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Export failed. Please check storage.", Toast.LENGTH_SHORT).show()
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
 * Saves a backup file directly to the device's public Downloads directory.
 */
private fun saveBackupToDownloads(
    context: Context,
    fileName: String,
    mimeType: String,
    dataWriter: (java.io.OutputStream) -> Boolean
): Boolean {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    } else {
        // Fallback for pre-Android 10
        @Suppress("DEPRECATION")
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SoftTodoTheme {
        SettingsScreenContent(
            activeTheme = "light",
            activeScheme = "minimal",
            onThemeChange = {},
            onSchemeChange = {},
            onExportJson = { true },
            onExportDb = { true },
            onImportBackup = { _, _ -> }
        )
    }
}
